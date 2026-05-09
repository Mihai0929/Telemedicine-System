--tabela abonamente
CREATE TABLE abonamente (
                            id_abonament SERIAL PRIMARY KEY,
                            nume_tip VARCHAR(50) NOT NULL UNIQUE,
                            pret DECIMAL(10, 2) NOT NULL CHECK (pret >= 0),
                            valabilitate_luni INT NOT NULL CHECK (valabilitate_luni > 0)
);

CREATE TABLE pacienti (
                          id_pacient SERIAL PRIMARY KEY,
                          nume_complet VARCHAR(150) NOT NULL,
                          email VARCHAR(100) NOT NULL UNIQUE,
                          parola_hash VARCHAR(255) NOT NULL,
                          data_nastere DATE NOT NULL,
                          nume_tutore VARCHAR(150),
                          id_abonament_activ INT,
                          data_expirare_abonament DATE,
                          FOREIGN KEY (id_abonament_activ) REFERENCES abonamente(id_abonament)
);

--istoric medical pacient
CREATE TABLE istoric_medical (
                                 id_inregistrare SERIAL PRIMARY KEY,
                                 id_pacient INT NOT NULL,
                                 afectiune VARCHAR(255) NOT NULL,
                                 data_inregistrare TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (id_pacient) REFERENCES pacienti(id_pacient) ON DELETE CASCADE
);

CREATE TABLE medici (
                        id_medic SERIAL PRIMARY KEY,
                        nume_medic VARCHAR(150) NOT NULL,
                        ora_inceput_tura TIME NOT NULL,
                        ora_sfarsit_tura TIME NOT NULL,
                        CHECK (ora_inceput_tura < ora_sfarsit_tura)
);

CREATE TABLE consultatii (
                             id_consultatie SERIAL PRIMARY KEY,
                             id_pacient INT NOT NULL,
                             id_medic INT, --in caz ca se genereaza automat reteta
                             data_ora_programare TIMESTAMP,
                             durata_minute INT CHECK (durata_minute IN (10, 15, 20, 30)),
                             diagnostic_provizoriu VARCHAR(255),
                             diagnostic_final VARCHAR(255),
                             status VARCHAR(50) NOT NULL DEFAULT 'IN_ASTEPTARE'
                                 CHECK (status IN ('IN_ASTEPTARE', 'PROGRAMAT', 'FINALIZAT', 'REDIRECTIONAT_URGENTE', 'RETETA_AUTOMATA')),
                             FOREIGN KEY (id_pacient) REFERENCES pacienti(id_pacient) ON DELETE CASCADE,
                             FOREIGN KEY (id_medic) REFERENCES medici(id_medic)
);

--simptome declarate pacient
CREATE TABLE simptome_initiale (
                                   id_simptom SERIAL PRIMARY KEY,
                                   id_consultatie INT NOT NULL,
                                   descriere_simptom VARCHAR(100) NOT NULL,
                                   FOREIGN KEY (id_consultatie) REFERENCES consultatii(id_consultatie) ON DELETE CASCADE
);

CREATE TABLE fise_evaluare (
                               id_fisa SERIAL PRIMARY KEY,
                               id_consultatie INT NOT NULL,
                               intrebare TEXT NOT NULL,
                               raspuns TEXT,
                               FOREIGN KEY (id_consultatie) REFERENCES consultatii(id_consultatie) ON DELETE CASCADE
);

CREATE TABLE retete (
                        id_reteta SERIAL PRIMARY KEY,
                        id_consultatie INT NOT NULL,
                        detalii_medicamente TEXT NOT NULL,
                        data_emitere TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (id_consultatie) REFERENCES consultatii(id_consultatie) ON DELETE CASCADE
);