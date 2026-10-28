package com.dsikorp.iamedassistan.agent;

import com.dsikorp.iamedassistan.dto.agent.AppointmentSelection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PendingBookingStoreTest {

    private final PendingBookingStore store = new PendingBookingStore();

    private static AppointmentSelection selection(String time) {
        return new AppointmentSelection("Cardiología", "2026-01-15", time, "Ana Martínez", "Turno disponible");
    }

    @Test
    @DisplayName("guarda y recupera la selección pendiente del usuario")
    void storesAndRetrievesSelection() {
        AppointmentSelection selection = selection("09:00");

        store.store(7L, selection);

        assertThat(store.retrieve(7L)).isEqualTo(selection);
    }

    @Test
    @DisplayName("no hay nada pendiente para un usuario desconocido")
    void returnsNullForUnknownUser() {
        assertThat(store.retrieve(99L)).isNull();
    }

    @Test
    @DisplayName("guardar de nuevo reemplaza la selección anterior del mismo usuario")
    void storingAgainOverwritesPreviousSelection() {
        store.store(7L, selection("09:00"));

        store.store(7L, selection("15:00"));

        assertThat(store.retrieve(7L)).isEqualTo(selection("15:00"));
    }

    @Test
    @DisplayName("los turnos pendientes de usuarios distintos no se pisan entre sí")
    void keepsSelectionsSeparatedByUser() {
        store.store(1L, selection("09:00"));
        store.store(2L, selection("10:00"));

        assertThat(store.retrieve(1L)).isEqualTo(selection("09:00"));
        assertThat(store.retrieve(2L)).isEqualTo(selection("10:00"));
    }

    @Test
    @DisplayName("remove devuelve la selección y la elimina del store")
    void removeReturnsAndClearsSelection() {
        store.store(7L, selection("09:00"));

        AppointmentSelection removed = store.remove(7L);

        assertThat(removed).isEqualTo(selection("09:00"));
        assertThat(store.retrieve(7L)).isNull();
    }

    @Test
    @DisplayName("remove sobre un usuario sin selección devuelve null")
    void removeReturnsNullWhenNothingPending() {
        assertThat(store.remove(99L)).isNull();
    }

    @Test
    @DisplayName("un remove posterior devuelve null porque ya no hay nada pendiente")
    void secondRemoveReturnsNull() {
        store.store(7L, selection("09:00"));

        store.remove(7L);

        assertThat(store.remove(7L)).isNull();
    }
}
