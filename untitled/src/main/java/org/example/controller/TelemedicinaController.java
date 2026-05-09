package org.example.controller;

import org.example.repository.ConsultatieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class TelemedicinaController {

    @Autowired
    private ConsultatieRepository consultatieRepository;

    @GetMapping("/")
    public String indexPagina(Model model) {
        model.addAttribute("programari", consultatieRepository.getUltimeleProgramari());
        model.addAttribute("fise", consultatieRepository.getFiseGenerate());
        return "index";
    }

    @PostMapping("/programeaza")
    public String faProgramare(@RequestParam int pacientId,
                               @RequestParam int complexitate,
                               @RequestParam(required = false, defaultValue = "") String simptom1,
                               @RequestParam(required = false, defaultValue = "") String simptom2,
                               @RequestParam(required = false, defaultValue = "") String simptom3,
                               Model model) {

        //trimit la baza de date
        String rezultat = consultatieRepository.programeazaAutomat(pacientId, complexitate, simptom1, simptom2, simptom3);

        model.addAttribute("mesajRezultat", rezultat);
        model.addAttribute("programari", consultatieRepository.getUltimeleProgramari());
        model.addAttribute("fise", consultatieRepository.getFiseGenerate());

        return "index";
    }

    @GetMapping("/dosar")
    public String veziDosarPacient(@RequestParam int pacientId, Model model) {
        model.addAttribute("programari", consultatieRepository.getUltimeleProgramari());
        model.addAttribute("fise", consultatieRepository.getFiseGenerate());

        // Căutăm datele pentru dosar
        Map<String, Object> detalii = consultatieRepository.getDetaliiPacient(pacientId);

        if (detalii != null) {
            model.addAttribute("dosarPacient", detalii);
            model.addAttribute("dosarAfectiuni", consultatieRepository.getAfectiuniPacient(pacientId));
            model.addAttribute("dosarConsultatii", consultatieRepository.getIstoricConsultatii(pacientId));
        } else {
            model.addAttribute("mesajRezultat", "EROARE: Pacientul cu ID-ul " + pacientId + " nu a fost găsit în baza de date.");
        }

        return "index";
    }

    @PostMapping("/finalizeaza")
    public String finalizeazaConsult(@RequestParam int idConsultatie,
                                     @RequestParam String diagnosticFinal,
                                     @RequestParam(required = false, defaultValue = "") String reteta,
                                     Model model) {

        String rezultat = consultatieRepository.finalizeazaConsultatia(idConsultatie, diagnosticFinal, reteta);

        model.addAttribute("mesajRezultat", rezultat);
        model.addAttribute("programari", consultatieRepository.getUltimeleProgramari());
        model.addAttribute("fise", consultatieRepository.getFiseGenerate());

        return "index";
    }
}