package com.parking.debug;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.parking.config.DatabaseConfig;

public class ShowUsers {

    public static void displayAllUsers() {

        String sql = "SELECT * FROM utilisateurs";

        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("=== LISTE DES UTILISATEURS ===");

            boolean hasData = false;

            while (rs.next()) {
                hasData = true;

                int id = rs.getInt("id_user");
                String login = rs.getString("login");
                String password = rs.getString("mot_de_passe");

                System.out.println("----------------------------");
                System.out.println("ID : " + id);
                System.out.println("Login : " + login);
                System.out.println("Mot de passe (hash) : " + password);
            }

            if (!hasData) {
                System.out.println("Aucun utilisateur trouvé.");
            }

        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des utilisateurs");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        displayAllUsers();
    }
}