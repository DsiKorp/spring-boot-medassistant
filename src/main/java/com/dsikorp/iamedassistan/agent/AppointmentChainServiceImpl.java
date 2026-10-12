package com.dsikorp.iamedassistan.agent;

import com.dsikorp.iamedassistan.service.AppointmentService;
import com.dsikorp.iamedassistan.config.ClientResolver;
import com.dsikorp.iamedassistan.dto.AppointmentInfo;
import com.dsikorp.iamedassistan.dto.agent.AppointmentRequest;
import com.dsikorp.iamedassistan.dto.agent.AppointmentSelection;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentChainServiceImpl implements AppointmentChainService{

    private final ClientResolver clientResolver;
    private final AppointmentService appointmentService;
    private final PendingBookingStore pendingBookingStore;

    @Value("classpath:prompts/appointment-extraction.st")
    private Resource extractionResource;

    @Value("classpath:prompts/appointment-confirmation.st")
    private Resource confirmationResource;

    private PromptTemplate extractionTemplate;
    private PromptTemplate confirmationTemplate;

    @PostConstruct
    void init(){
        extractionTemplate = new PromptTemplate(extractionResource);
        confirmationTemplate = new PromptTemplate(confirmationResource);
    }

    @Override
    public String bookAppointmentChain(String userRequest, String model, Long userId) {
        AppointmentRequest extracted = interpret(userRequest, model);
        List<AppointmentInfo> available = search(extracted);

        return select(userRequest, available, model, userId);
    }

//    @Override
//    public String confirmBooking(Long userId) {
//        AppointmentSelection selection = pendingBookingStore.remove(userId);
//
//        if(selection==null){
//            return "No tenés ningún turno pendiente de confirmación.";
//        }
//
//        if(!isStillAvailable(selection)){
//            log.info("Turno ya no disponible para userId={}: {} {} {}",
//                    userId, selection.specialty(), selection.date(), selection.time());
//            return "El turno que seleccionaste ya no está disponible. Podés buscar otro.";
//        }
//
//        return book(selection,userId);
//    }

//    private String book(AppointmentSelection selection, Long userId){
//
//        String result = appointmentService.bookAppointment(
//                selection.specialty(),
//                LocalDate.parse(selection.date()),
//                LocalTime.parse(selection.time()),
//                userId
//        );
//
//        log.info("Resultado de la reserva: {}", result);
//        return result;
//    }
//
//    private boolean isStillAvailable(AppointmentSelection selection){
//        List<AppointmentInfo> available = appointmentService.findAvailableAppointments(
//                selection.specialty(), LocalDate.parse(selection.date()));
//
//        return available.stream().anyMatch( a -> a.time().equals(selection.time()));
//    }


//    @Override
//    public String cancelBooking(Long userId) {
//
//        AppointmentSelection selection = pendingBookingStore.remove(userId);
//
//        if(selection==null){
//            return "No tenés ningún turno pendiente de cancelación.";
//        }
//        log.info("Turno cancelado por userId={}: {} {} {}",
//                userId, selection.specialty(), selection.date(), selection.time());
//
//        return "Turno descartado. Podés buscar otro cuando quieras.";
//    }


    private AppointmentRequest interpret(String userRequest, String model){

        String prompt = extractionTemplate.render(Map.of("pedido", userRequest));

//        AppointmentRequest extracted = ChatClient.create(clientResolver.resolveModel(model))
        AppointmentRequest extracted = clientResolver.resolve(model)
                .prompt()
                .system("Sos un extractor de datos. Extraés exactamente lo que dice el texto, " +
                        "sin interpretar ni cambiar nada.")
                .user(prompt)
                .call()
                .entity(AppointmentRequest.class);

        log.info("Chain paso 1 — extraído: specialty={}, date={}", extracted.specialty(), extracted.date());
        return extracted;
    }

    private List<AppointmentInfo> search(AppointmentRequest extracted){

        List<AppointmentInfo> available = appointmentService.findAvailableAppointments(
                extracted.specialty(), LocalDate.parse(extracted.date())
        );

        log.info("Chain paso 2 — turnos encontrados: {}", available.size());
        log.info("Chain paso 2 — extracted.specialty(): {}", extracted.specialty());
        log.info("Chain paso 2 — LocalDate.parse(extracted.date(): {}", LocalDate.parse(extracted.date()));
        return available;
    }


    private String select(String userRequest, List<AppointmentInfo> available,
                          String model, Long userId){

        if(available.isEmpty()){
            return "No se encontraron turnos disponibles para esa especialidad y fecha. Podés probar con otra fecha.";
        }

        AppointmentSelection selection = selectAppointment(userRequest,available, model);

        pendingBookingStore.store(userId, selection);

        return selection.message();

    }

    private AppointmentSelection selectAppointment(String userRequest, List<AppointmentInfo> available, String model){
        String prompt = confirmationTemplate.render(Map.of(
                "pedido", userRequest,
                "turnos", available.toString()
        ));

        log.info("Chain paso 3 - turnos enviados al modelo: {}", available);

        //return ChatClient.create(clientResolver.resolveModel(model))
        return clientResolver.resolve(model)
                .prompt()
                .system("Sos un asistente de gestión de turnos médicos. " +
                        "Seleccioná el mejor turno y completá todos los campos.")
                .user(prompt)
                .call()
                .entity(AppointmentSelection.class);
    }


}











