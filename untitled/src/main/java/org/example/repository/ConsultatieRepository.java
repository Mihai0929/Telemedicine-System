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
}