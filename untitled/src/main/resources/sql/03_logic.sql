--verificam daca are tutor declarat
CREATE OR REPLACE FUNCTION check_tutore_minor_func()
RETURNS TRIGGER AS $$
DECLARE
varsta INT;
BEGIN
    --calculez varsta
    varsta := EXTRACT(YEAR FROM age(NEW.data_nastere));

    --daca e minor si nu are tutore iesim
    IF varsta < 18 AND (NEW.nume_tutore IS NULL OR TRIM(NEW.nume_tutore) = '') THEN
        RAISE EXCEPTION 'Pacientul este minor (are % ani). Numele tutorelui este obligatoriu.', varsta
            USING ERRCODE = 'P0001';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifica_tutore
    BEFORE INSERT OR UPDATE ON pacienti
                         FOR EACH ROW
                         EXECUTE FUNCTION check_tutore_minor_func();

--verificam daca are abonament activ pacientul
CREATE OR REPLACE FUNCTION check_abonament_activ_func()
RETURNS TRIGGER AS $$
DECLARE
expirare DATE;
BEGIN
SELECT data_expirare_abonament INTO expirare
FROM pacienti WHERE id_pacient = NEW.id_pacient;

--daca are abonament expirat / nu are
IF expirare IS NULL OR expirare < CURRENT_DATE THEN
        RAISE EXCEPTION 'Pacientul % nu are un abonament activ!', NEW.id_pacient
            USING ERRCODE = 'P0002';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifica_abonament
    BEFORE INSERT ON consultatii
    FOR EACH ROW
    EXECUTE FUNCTION check_abonament_activ_func();

--programare automata--

DROP FUNCTION IF EXISTS programeaza_pacient_automat;

CREATE OR REPLACE FUNCTION programeaza_pacient_automat(
    p_id_pacient INT,
    p_nivel_complexitate INT,
    p_s1 VARCHAR,
    p_s2 VARCHAR,
    p_s3 VARCHAR
)
RETURNS TABLE(mesaj VARCHAR, ora_gasita TIMESTAMP, medic_alocat VARCHAR, id_consult_generat INT) AS $$
DECLARE
v_durata_necesara INT;
    v_medic RECORD;
    v_ora_curenta TIMESTAMP;
    v_ora_test TIMESTAMP;
    v_este_liber BOOLEAN;
    v_id_consultatie INT;

    v_varsta INT;
    v_istoric TEXT;
    v_simptome_text TEXT;
BEGIN
    IF p_nivel_complexitate = 1 THEN v_durata_necesara := 10;
    ELSIF p_nivel_complexitate = 2 THEN v_durata_necesara := 15;
ELSE v_durata_necesara := 30;
END IF;

    v_ora_curenta := CURRENT_TIMESTAMP;

    --cautam medic disponibil
FOR v_medic IN SELECT id_medic, nume_medic, ora_inceput_tura, ora_sfarsit_tura FROM medici LOOP
    v_ora_test := CURRENT_DATE + v_medic.ora_inceput_tura;

WHILE v_ora_test + (v_durata_necesara || ' minutes')::interval <= CURRENT_DATE + v_medic.ora_sfarsit_tura LOOP
            IF v_ora_test < v_ora_curenta THEN
                v_ora_test := v_ora_test + INTERVAL '15 minutes'; CONTINUE;
END IF;

SELECT NOT EXISTS (
    SELECT 1 FROM consultatii c WHERE c.id_medic = v_medic.id_medic
                                  AND c.data_ora_programare < v_ora_test + (v_durata_necesara || ' minutes')::interval
        AND c.data_ora_programare + (c.durata_minute || ' minutes')::interval > v_ora_test
) INTO v_este_liber;

IF v_este_liber THEN
                --inseram in consultatii
                INSERT INTO consultatii (id_pacient, id_medic, data_ora_programare, durata_minute, status, diagnostic_provizoriu)
                VALUES (p_id_pacient, v_medic.id_medic, v_ora_test, v_durata_necesara, 'PROGRAMAT', 'Triaj în curs de completare...')
                RETURNING id_consultatie INTO v_id_consultatie;

                --salvam simptomele in tabela
                IF p_s1 != '' THEN INSERT INTO simptome_initiale(id_consultatie, descriere_simptom) VALUES (v_id_consultatie, p_s1); END IF;
                IF p_s2 != '' THEN INSERT INTO simptome_initiale(id_consultatie, descriere_simptom) VALUES (v_id_consultatie, p_s2); END IF;
                IF p_s3 != '' THEN INSERT INTO simptome_initiale(id_consultatie, descriere_simptom) VALUES (v_id_consultatie, p_s3); END IF;

                --extragem datele pacientului
SELECT EXTRACT(YEAR FROM age(data_nastere)) INTO v_varsta FROM pacienti WHERE id_pacient = p_id_pacient;
SELECT string_agg(LOWER(afectiune), ' ') INTO v_istoric FROM istoric_medical WHERE id_pacient = p_id_pacient;
IF v_istoric IS NULL THEN v_istoric := ''; END IF;

                --concatenam simptome sa gasim cuvinte cheie
                v_simptome_text := LOWER(p_s1 || ' ' || p_s2 || ' ' || p_s3);

                --dureri abdominale + febra
                IF v_simptome_text LIKE '%febra%' AND v_simptome_text LIKE '%abdominal%' THEN
                    IF v_varsta < 18 THEN
                        INSERT INTO fise_evaluare(id_consultatie, intrebare) VALUES (v_id_consultatie, 'Copilul a avut vărsături în ultimele 12 ore? (Verificare infecție digestivă)');
INSERT INTO fise_evaluare(id_consultatie, intrebare) VALUES (v_id_consultatie, 'Are poftă de mâncare?');
ELSE
                        INSERT INTO fise_evaluare(id_consultatie, intrebare) VALUES (v_id_consultatie, 'Durerea este ascuțită și localizată în partea dreaptă jos? (Risc crescut de Apendicită - Direcționare Urgențe)');
END IF;
END IF;

                --tuse + simptome din trecut
                IF v_simptome_text LIKE '%tuse%' THEN
                    INSERT INTO fise_evaluare(id_consultatie, intrebare) VALUES (v_id_consultatie, 'Tusea este seacă sau productivă?');
                    IF v_istoric LIKE '%astm%' THEN
                        INSERT INTO fise_evaluare(id_consultatie, intrebare) VALUES (v_id_consultatie, 'Atenție (Pacient cu istoric de Astm): Ați folosit inhalatorul de urgență astăzi?');
END IF;
END IF;

                IF NOT EXISTS (SELECT 1 FROM fise_evaluare WHERE id_consultatie = v_id_consultatie) THEN
                    INSERT INTO fise_evaluare(id_consultatie, intrebare) VALUES (v_id_consultatie, 'De cât timp au apărut exact aceste simptome?');
INSERT INTO fise_evaluare(id_consultatie, intrebare) VALUES (v_id_consultatie, 'Ați luat vreun medicament în ultimele 24h?');
END IF;

RETURN QUERY SELECT 'Programare și Triaj realizate!'::VARCHAR, v_ora_test::TIMESTAMP, v_medic.nume_medic::VARCHAR, v_id_consultatie::INT;
RETURN;
END IF;

            v_ora_test := v_ora_test + INTERVAL '15 minutes';
END LOOP;
END LOOP;

    RAISE EXCEPTION 'Nu am găsit niciun loc liber azi!' USING ERRCODE = 'P0003';
END;

CREATE OR REPLACE FUNCTION finalizeaza_consult(
    p_id_consult INT,
    p_diagnostic_final VARCHAR,
    p_reteta TEXT
) RETURNS VARCHAR AS $$
BEGIN
    --actualizare consulltatie
UPDATE consultatii
SET status = 'FINALIZAT',
    diagnostic_final = p_diagnostic_final
WHERE id_consultatie = p_id_consult;

IF p_reteta IS NOT NULL AND p_reteta != '' THEN
        INSERT INTO retete (id_consultatie, detalii_medicamente)
        VALUES (p_id_consult, p_reteta);
END IF;

RETURN 'Consultația #' || p_id_consult || ' a fost finalizată cu succes!';
END;
$$ LANGUAGE plpgsql;