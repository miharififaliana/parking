package com.parking.dao;

import com.parking.model.Log;
import com.parking.model.Place;
import com.parking.model.Role;
import com.parking.model.SequenceTicket;
import com.parking.model.StatutPlace;
import com.parking.model.StatutTicket;
import com.parking.model.Ticket;
import com.parking.model.Utilisateur;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Utilitaires de mapping JDBC → entités du domaine.
 */
final class JdbcMapper {

    private JdbcMapper() {
    }

    static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    static Timestamp toTimestamp(LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }

    static Place mapPlace(ResultSet rs) throws SQLException {
        Place place = new Place();
        place.setIdPlace(rs.getInt("id_place"));
        place.setNumeroPlace(rs.getInt("numero_place"));
        place.setStatut(StatutPlace.fromDbValue(rs.getString("statut")));
        place.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        place.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return place;
    }

    static Ticket mapTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setIdTicket(rs.getInt("id_ticket"));
        ticket.setNumeroTicket(rs.getString("numero_ticket"));
        ticket.setIdPlace(rs.getInt("id_place"));
        ticket.setImmatriculation(rs.getString("immatriculation"));
        ticket.setDateEntree(toLocalDateTime(rs.getTimestamp("date_entree")));
        ticket.setDateSortie(toLocalDateTime(rs.getTimestamp("date_sortie")));
        BigDecimal montant = rs.getBigDecimal("montant");
        ticket.setMontant(rs.wasNull() ? null : montant);
        ticket.setStatut(StatutTicket.fromDbValue(rs.getString("statut")));
        ticket.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        ticket.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return ticket;
    }

    static Utilisateur mapUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setIdUser(rs.getInt("id_user"));
        utilisateur.setNom(rs.getString("nom"));
        utilisateur.setLogin(rs.getString("login"));
        utilisateur.setMotDePasse(rs.getString("mot_de_passe"));
        utilisateur.setRole(Role.fromDbValue(rs.getString("role")));
        utilisateur.setActif(rs.getBoolean("actif"));
        utilisateur.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        utilisateur.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return utilisateur;
    }

    static Log mapLog(ResultSet rs) throws SQLException {
        Log log = new Log();
        log.setIdLog(rs.getInt("id_log"));
        log.setIdUser(rs.getInt("id_user"));
        log.setAction(rs.getString("action"));
        log.setDetail(rs.getString("detail"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return log;
    }

    static SequenceTicket mapSequenceTicket(ResultSet rs) throws SQLException {
        SequenceTicket sequence = new SequenceTicket();
        sequence.setAnnee(rs.getInt("annee"));
        sequence.setDerniereSequence(rs.getInt("derniere_sequence"));
        sequence.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return sequence;
    }
}
