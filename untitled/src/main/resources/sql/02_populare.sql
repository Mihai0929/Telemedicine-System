DO $$
DECLARE
i INT;
    random_medic_id INT;
    random_pacient_id INT;
    random_consult_id INT;
BEGIN
FOR i IN 1..15 LOOP
        INSERT INTO abonamente (nume_tip, pret, valabilitate_luni)
        VALUES ('Abonament Tip ' || i, (i * 10.50), CASE WHEN i % 2 = 0 THEN 1 ELSE 12 END)
        ON CONFLICT DO NOTHING;
END LOOP;

FOR i IN 1..15 LOOP
        INSERT INTO pacienti (nume_complet, email, parola_hash, data_nastere, id_abonament_activ, data_expirare_abonament)
        VALUES (
            'Pacient Test ' || i,
            'pacient' || i || '@test.com',
            'hash_dummy',
            CURRENT_DATE - (INTERVAL '1 year' * (10 + (random() * 50)::int)),
            (SELECT id_abonament FROM abonamente ORDER BY random() LIMIT 1),
            CURRENT_DATE + INTERVAL '1 month'
        ) ON CONFLICT DO NOTHING;
END LOOP;

--istoric medical
FOR i IN 1..15 LOOP
SELECT id_pacient INTO random_pacient_id FROM pacienti ORDER BY random() LIMIT 1;
INSERT INTO istoric_medical (id_pacient, afectiune)
VALUES (random_pacient_id, 'Afectiune generica ' || (random() * 100)::int);
END LOOP;

--medici
FOR i IN 1..15 LOOP
        INSERT INTO medici (nume_medic, ora_inceput_tura, ora_sfarsit_tura)
        VALUES (
            'Dr. Medic ' || i,
            MAKE_TIME(8 + (i % 4), 0, 0),
            MAKE_TIME(14 + (i % 4), 0, 0)
        );
END LOOP;

--consultatii
FOR i IN 1..15 LOOP
SELECT id_pacient INTO random_pacient_id FROM pacienti ORDER BY random() LIMIT 1;
SELECT id_medic INTO random_medic_id FROM medici ORDER BY random() LIMIT 1;

INSERT INTO consultatii (id_pacient, id_medic, data_ora_progrwamare, durata_minute, status)
VALUES (
           random_pacient_id,
           random_medic_id,
           CURRENT_TIMESTAMP + (INTERVAL '1 day' * (random() * 5)::int),
           CASE (random() * 3)::int WHEN 0 THEN 10 WHEN 1 THEN 15 WHEN 2 THEN 20 ELSE 30 END,
           'PROGRAMAT'
       );
END LOOP;

--simptome initiale
FOR i IN 1..20 LOOP
SELECT id_consultatie INTO random_consult_id FROM consultatii ORDER BY random() LIMIT 1;
INSERT INTO simptome_initiale (id_consultatie, descriere_simptom)
VALUES (random_consult_id, 'Simptom test ' || i);
END LOOP;

--fise evaluare
FOR i IN 1..20 LOOP
SELECT id_consultatie INTO random_consult_id FROM consultatii ORDER BY random() LIMIT 1;
INSERT INTO fise_evaluare (id_consultatie, intrebare, raspuns)
VALUES (random_consult_id, 'Intrebare generata ' || i || '?', 'Raspuns test ' || i);
END LOOP;

--retete
FOR i IN 1..15 LOOP
SELECT id_consultatie INTO random_consult_id FROM consultatii ORDER BY random() LIMIT 1;
INSERT INTO retete (id_consultatie, detalii_medicamente)
VALUES (random_consult_id, 'Medicament X - ' || (random() * 3)::int || ' pe zi');
END LOOP;

END $$;