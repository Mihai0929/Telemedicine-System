package org.example.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class ConsultatieRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getUltimeleProgramari() {
        try {
            String sql = "SELECT c.id_consultatie, p.nume_complet AS pacient, " +
                    "COALESCE(m.nume_medic, 'Sistem (Automat)') AS medic, " +
                    "TO_CHAR(c.data_ora_programare, 'DD.MM.YYYY HH24:MI') AS data_formatata, " +
                    "c.durata_minute, c.status " +
                    "FROM consultatii c " +
                    "JOIN pacienti p ON c.id_pacient = p.id_pacient " +
                    "LEFT JOIN medici m ON c.id_medic = m.id_medic " +
                    "ORDER BY c.id_consultatie DESC LIMIT 10";

            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            System.err.println("Eroare la preluarea programarilor: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public String programeazaAutomat(int pacientId, int complexityLevel, String s1, String s2, String s3) {
        String sql = "SELECT * FROM programeaza_pacient_automat(?, ?, ?, ?, ?)";

        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, pacientId, complexityLevel, s1, s2, s3);
            String mesaj = (String) result.get("mesaj");
            Object ora = result.get("ora_gasita");
            String medic = (String) result.get("medic_alocat");

            return "SUCCES: " + mesaj + " Alocat la: " + medic + " (" + ora.toString() + ")";
        } catch (DataAccessException e) {
            String err = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            if (err.contains("ERROR:")) err = err.split("ERROR:")[1].split("\n")[0].trim();
            return "EROARE DIN BAZA DE DATE: " + err;
        }
    }

    public List<Map<String, Object>> getFiseGenerate() {
        try {
            String sql = "SELECT fe.id_fisa, c.id_consultatie, p.nume_complet, fe.intrebare " +
                    "FROM fise_evaluare fe " +
                    "JOIN consultatii c ON fe.id_consultatie = c.id_consultatie " +
                    "JOIN pacienti p ON c.id_pacient = p.id_pacient " +
                    "WHERE fe.raspuns IS NULL " +
                    "ORDER BY fe.id_fisa DESC LIMIT 5";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getDetaliiPacient(int pacientId) {
        try {
            String sql = "SELECT p.id_pacient, p.nume_complet, " +
                    "EXTRACT(YEAR FROM age(p.data_nastere)) AS varsta, " +
                    "a.nume_tip AS tip_abonament, " +
                    "TO_CHAR(p.data_expirare_abonament, 'DD.MM.YYYY') AS data_expirare " +
                    "FROM pacienti p " +
                    "LEFT JOIN abonamente a ON p.id_abonament_activ = a.id_abonament " +
                    "WHERE p.id_pacient = ?";
            return jdbcTemplate.queryForMap(sql, pacientId);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getAfectiuniPacient(int pacientId) {
        String sql = "SELECT afectiune FROM istoric_medical WHERE id_pacient = ?";
        return jdbcTemplate.queryForList(sql, new Object[]{pacientId}, String.class);
    }

    public List<Map<String, Object>> getIstoricConsultatii(int pacientId) {
        String sql = "SELECT TO_CHAR(c.data_ora_programare, 'DD.MM.YYYY HH24:MI') AS data_c, " +
                "COALESCE(m.nume_medic, 'Automat (Sistem)') AS medic, " +
                "c.diagnostic_provizoriu, " +
                "r.detalii_medicamente " +
                "FROM consultatii c " +
                "LEFT JOIN medici m ON c.id_medic = m.id_medic " +
                "LEFT JOIN retete r ON c.id_consultatie = r.id_consultatie " +
                "WHERE c.id_pacient = ? " +
                "ORDER BY c.data_ora_programare DESC";
        return jdbcTemplate.queryForList(sql, pacientId);
    }

    public String finalizeazaConsultatia(int idConsult, String diagnostic, String reteta) {
        try {
            String sql = "SELECT finalizeaza_consult(?, ?, ?)";
            return jdbcTemplate.queryForObject(sql, String.class, idConsult, diagnostic, reteta);
        } catch (Exception e) {
            return "EROARE LA FINALIZARE: " + e.getMessage();
        }
    }

    public List<Map<String, Object>> getTopSimptome() {
        try {
            String sql = "SELECT LOWER(descriere_simptom) as simptom, COUNT(id_simptom) as total " +
                    "FROM simptome_initiale " +
                    "GROUP BY LOWER(descriere_simptom) " +
                    "ORDER BY total DESC LIMIT 5";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getIncarcareMedici() {
        try {
            String sql = "SELECT COALESCE(m.nume_medic, 'Sistem Automat') as medic, COUNT(c.id_consultatie) as total_consultatii " +
                    "FROM consultatii c " +
                    "LEFT JOIN medici m ON c.id_medic = m.id_medic " +
                    "GROUP BY m.nume_medic " +
                    "ORDER BY total_consultatii DESC";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public String inregistreazaPacient(String nume, String email, String parola, java.time.LocalDate dataNastere, String tutore) {
        String sql = "INSERT INTO pacienti (nume_complet, email, parola_hash, data_nastere, nume_tutore) VALUES (?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql, nume, email, parola, dataNastere, tutore.isEmpty() ? null : tutore);
            return "SUCCES: Contul a fost creat! Te poți autentifica.";
        } catch (DataAccessException e) {
            String err = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            if (err.contains("ERROR:")) err = err.split("ERROR:")[1].split("\n")[0].trim();
            if (err.toLowerCase().contains("unique constraint") || err.toLowerCase().contains("duplicate key")) {
                return "EROARE: Există deja un cont creat cu acest email!";
            }
            return "EROARE DIN BAZA DE DATE: " + err;
        }
    }

    public Integer autentificaPacient(String email, String parola) {
        try {
            return jdbcTemplate.queryForObject("SELECT id_pacient FROM pacienti WHERE email = ? AND parola_hash = ?", Integer.class, email, parola);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> getToateAbonamentele() {
        try {
            return jdbcTemplate.queryForList("SELECT * FROM abonamente ORDER BY pret ASC");
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public String cumparaAbonament(int idPacient, int idAbonament) {
        String sql = "UPDATE pacienti " +
                "SET id_abonament_activ = ?, " +
                "    data_expirare_abonament = CURRENT_DATE + (SELECT valabilitate_luni * INTERVAL '1 month' FROM abonamente WHERE id_abonament = ?) " +
                "WHERE id_pacient = ?";
        try {

            jdbcTemplate.update(sql, idAbonament, idAbonament, idPacient);
            return "SUCCES: Abonamentul a fost activat/prelungit cu succes!";
        } catch (Exception e) {
            return "EROARE LA PLATA ABONAMENTULUI: " + e.getMessage();
        }
    }

    public List<Map<String, Object>> getProgramariPersonalizate(int pacientId) {
        try {
            String sql = "SELECT c.id_consultatie, p.nume_complet AS pacient, " +
                    "COALESCE(m.nume_medic, 'Sistem (Automat)') AS medic, " +
                    "TO_CHAR(c.data_ora_programare, 'DD.MM.YYYY HH24:MI') AS data_formatata, " +
                    "c.durata_minute, c.status " +
                    "FROM consultatii c " +
                    "JOIN pacienti p ON c.id_pacient = p.id_pacient " +
                    "LEFT JOIN medici m ON c.id_medic = m.id_medic " +
                    "WHERE c.id_pacient = ? " + // Filtru: doar ale mele
                    "ORDER BY c.id_consultatie DESC";
            return jdbcTemplate.queryForList(sql, pacientId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public String anuleazaProgramare(int idConsultatie) {
        String sql = "UPDATE consultatii SET status = 'ANULAT' WHERE id_consultatie = ?";
        try {
            jdbcTemplate.update(sql, idConsultatie);
            return "SUCCES: Programarea #" + idConsultatie + " a fost anulată.";
        } catch (Exception e) {
            return "EROARE LA ANULARE: " + e.getMessage();
        }
    }
}