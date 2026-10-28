package com.dsikorp.iamedassistan.agent;

import com.dsikorp.iamedassistan.ChatClientTestSupport;
import com.dsikorp.iamedassistan.config.ClientResolver;
import com.dsikorp.iamedassistan.dto.AppointmentInfo;
import com.dsikorp.iamedassistan.dto.agent.AppointmentRequest;
import com.dsikorp.iamedassistan.dto.agent.AppointmentSelection;
import com.dsikorp.iamedassistan.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentChainServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final LocalDate FECHA = LocalDate.of(2026, 1, 15);

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private ChatModel chatModel;

    private PendingBookingStore pendingBookingStore;
    private AppointmentChainServiceImpl service;

    @BeforeEach
    void setUp() {
        pendingBookingStore = new PendingBookingStore();
        service = new AppointmentChainServiceImpl(clientResolver, appointmentService, pendingBookingStore);

        ReflectionTestUtils.setField(service, "extractionResource", new ClassPathResource("prompts/appointment-extraction.st"));
        ReflectionTestUtils.setField(service, "confirmationResource", new ClassPathResource("prompts/appointment-confirmation.st"));

        service.init();
    }

    private static AppointmentSelection selection(String time) {
        return new AppointmentSelection("Cardiología", "2026-01-15", time, "Ana Martínez",
                "Turno de cardiología el 2026-01-15 a las " + time);
    }

    @Test
    @DisplayName("la cadena extrae, busca y deja el turno seleccionado pendiente de confirmación")
    void chainStoresSelectedAppointmentAsPending() {
        AppointmentSelection selected = selection("09:00");

        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().entity(AppointmentRequest.class))
                    .thenReturn(new AppointmentRequest("Cardiología", "2026-01-15"));
            when(mock.call().entity(AppointmentSelection.class)).thenReturn(selected);
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            when(appointmentService.findAvailableAppointments("Cardiología", FECHA))
                    .thenReturn(List.of(new AppointmentInfo("Ana Martínez", "Cardiología", "2026-01-15", "09:00")));

            String response = service.bookAppointmentChain("quiero cardiología el 15/01/2026", "gemini", USER_ID);

            assertThat(response).isEqualTo(selected.message());
            assertThat(pendingBookingStore.retrieve(USER_ID)).isEqualTo(selected);
            verify(appointmentService).findAvailableAppointments("Cardiología", FECHA);
        }
    }

    @Test
    @DisplayName("si no hay turnos disponibles la cadena lo informa y no deja nada pendiente")
    void chainReportsNoAvailableAppointments() {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().entity(AppointmentRequest.class))
                    .thenReturn(new AppointmentRequest("Cardiología", "2026-01-15"));
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            when(appointmentService.findAvailableAppointments("Cardiología", FECHA)).thenReturn(List.of());

            String response = service.bookAppointmentChain("quiero cardiología", "gemini", USER_ID);

            assertThat(response).contains("No se encontraron turnos disponibles");
            assertThat(pendingBookingStore.retrieve(USER_ID)).isNull();
        }
    }

    @Test
    @DisplayName("confirmar sin turno pendiente informa que no hay nada pendiente")
    void confirmWithoutPendingBookingReportsNothingPending() {
        String response = service.confirmBooking(USER_ID);

        assertThat(response).isEqualTo("No tenés ningún turno pendiente de confirmación.");
        verify(appointmentService, never()).bookAppointment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("confirmar un turno que ya no está disponible lo informa y no lo reserva")
    void confirmReportsSlotNoLongerAvailable() {
        pendingBookingStore.store(USER_ID, selection("09:00"));

        when(appointmentService.findAvailableAppointments("Cardiología", FECHA))
                .thenReturn(List.of(new AppointmentInfo("Ana Martínez", "Cardiología", "2026-01-15", "15:00")));

        String response = service.confirmBooking(USER_ID);

        assertThat(response).isEqualTo("El turno que seleccionaste ya no está disponible. Podés buscar otro.");
        verify(appointmentService, never()).bookAppointment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("confirmar un turno disponible lo persiste y devuelve el resultado del servicio")
    void confirmBooksPendingAppointment() {
        pendingBookingStore.store(USER_ID, selection("09:00"));

        when(appointmentService.findAvailableAppointments("Cardiología", FECHA))
                .thenReturn(List.of(new AppointmentInfo("Ana Martínez", "Cardiología", "2026-01-15", "09:00")));
        when(appointmentService.bookAppointment("Cardiología", FECHA, LocalTime.of(9, 0), USER_ID))
                .thenReturn("Turno reservado exitosamente. 2026-01-15 09:00");

        String response = service.confirmBooking(USER_ID);

        assertThat(response).isEqualTo("Turno reservado exitosamente. 2026-01-15 09:00");
        verify(appointmentService).bookAppointment("Cardiología", FECHA, LocalTime.of(9, 0), USER_ID);
        assertThat(pendingBookingStore.retrieve(USER_ID)).isNull();
    }

    @Test
    @DisplayName("cancelar sin turno pendiente informa que no hay nada pendiente")
    void cancelWithoutPendingBookingReportsNothingPending() {
        String response = service.cancelBooking(USER_ID);

        assertThat(response).isEqualTo("No tenés ningún turno pendiente de cancelación.");
    }

    @Test
    @DisplayName("cancelar descarta el turno pendiente")
    void cancelDiscardsPendingAppointment() {
        pendingBookingStore.store(USER_ID, selection("09:00"));

        String response = service.cancelBooking(USER_ID);

        assertThat(response).isEqualTo("Turno descartado. Podés buscar otro cuando quieras.");
        assertThat(pendingBookingStore.retrieve(USER_ID)).isNull();
        verify(appointmentService, never()).bookAppointment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("la extracción se pide al modelo con el pedido original del paciente")
    void extractionUsesOriginalPatientRequest() {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().entity(AppointmentRequest.class))
                    .thenReturn(new AppointmentRequest("Cardiología", "2026-01-15"));
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            when(appointmentService.findAvailableAppointments("Cardiología", FECHA)).thenReturn(List.of());

            service.bookAppointmentChain("necesito un turno de cardiología para el 15 de enero", "gemini", USER_ID);

            ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).user(userPrompt.capture());
            assertThat(userPrompt.getValue())
                    .contains("necesito un turno de cardiología para el 15 de enero");
        }
    }
}
