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

    private void incarcaDateDashboard(Model model) {
        model.addAttribute("programari", consultatieRepository.getUltimeleProgramari());
        model.addAttribute("fise", consultatieRepository.getFiseGenerate());
        model.addAttribute("topSimptome", consultatieRepository.getTopSimptome());
        model.addAttribute("incarcareMedici", consultatieRepository.getIncarcareMedici());
    }

    @GetMapping("/")
    public String indexPagina(jakarta.servlet.http.HttpSession session, Model model) {
        if (session.getAttribute("userLogat") == null) {
            return "redirect:/login";
        }
        incarcaDateDashboard(model);
        return "index";
    }

    @GetMapping("/dosar")
    public String veziDosarPacient(@RequestParam int pacientId, Model model) {
        incarcaDateDashboard(model);

        Map<String, Object> detalii = consultatieRepository.getDetaliiPacient(pacientId);
        if (detalii != null) {
            model.addAttribute("dosarPacient", detalii);
            model.addAttribute("dosarAfectiuni", consultatieRepository.getAfectiuniPacient(pacientId));
            model.addAttribute("dosarConsultatii", consultatieRepository.getIstoricConsultatii(pacientId));
        } else {
            model.addAttribute("mesajRezultat", "EROARE: Pacientul " + pacientId + " nu există.");
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
        incarcaDateDashboard(model);
        return "index";
    }

    @GetMapping("/login")
    public String paginaLogin() {
        return "login";
    }

    @PostMapping("/register")
    public String faInregistrare(@RequestParam String numeComplet,
                                 @RequestParam String email,
                                 @RequestParam String parola,
                                 @RequestParam String dataNastere,
                                 @RequestParam(required = false, defaultValue = "") String tutore,
                                 Model model) {
        java.time.LocalDate data = java.time.LocalDate.parse(dataNastere);

        String rezultat = consultatieRepository.inregistreazaPacient(numeComplet, email, parola, data, tutore);
        model.addAttribute("mesajRezultat", rezultat);
        return "login";
    }

    @PostMapping("/dologin")
    public String faLogin(@RequestParam String email,
                          @RequestParam String parola,
                          jakarta.servlet.http.HttpSession session,
                          Model model) {
        Integer idPacient = consultatieRepository.autentificaPacient(email, parola);

        if (idPacient != null) {
            session.setAttribute("userLogat", idPacient);
            return "redirect:/";
        } else {
            model.addAttribute("mesajRezultat", "EROARE: Email sau parolă incorecte!");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String faLogout(jakarta.servlet.http.HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @PostMapping("/programeaza")
    public String faProgramare(jakarta.servlet.http.HttpSession session,
                               @RequestParam int complexitate,
                               @RequestParam(required = false, defaultValue = "") String simptom1,
                               @RequestParam(required = false, defaultValue = "") String simptom2,
                               @RequestParam(required = false, defaultValue = "") String simptom3,
                               Model model) {

        //preiau id-ul de la utilizator
        Integer pacientId = (Integer) session.getAttribute("userLogat");

        if (pacientId == null) {
            return "redirect:/login";
        }

        String rezultat = consultatieRepository.programeazaAutomat(pacientId, complexitate, simptom1, simptom2, simptom3);

        model.addAttribute("mesajRezultat", rezultat);
        incarcaDateDashboard(model);
        return "index";
    }

    @GetMapping("/dosarul-meu")
    public String veziDosarulMeu(jakarta.servlet.http.HttpSession session, Model model) {
        Integer pacientId = (Integer) session.getAttribute("userLogat");
        if (pacientId == null) return "redirect:/login";

        incarcaDateDashboard(model);

        Map<String, Object> detalii = consultatieRepository.getDetaliiPacient(pacientId);
        if (detalii != null) {
            model.addAttribute("dosarPacient", detalii);
            model.addAttribute("dosarAfectiuni", consultatieRepository.getAfectiuniPacient(pacientId));
            model.addAttribute("dosarConsultatii", consultatieRepository.getIstoricConsultatii(pacientId));
        } else {
            model.addAttribute("mesajRezultat", "EROARE: Nu s-a putut încărca dosarul.");
        }
        return "index";
    }
}