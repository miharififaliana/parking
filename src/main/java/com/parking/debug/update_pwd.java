package com.parking.debug;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.parking.config.DatabaseConfig;
import org.mindrot.jbcrypt.BCrypt;

public class update_pwd {

    public static void updatePassword(int userId, String newPassword) {

        String sql = "UPDATE utilisateurs SET mot_de_passe = ? WHERE id_user = ?";

        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // 1. Hash du mot de passe
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

            // 2. UPDATE
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✔ UPDATE réussi en base");
                con.commit();
                // 3. Vérification après update
                verifyPassword(con, userId, newPassword);

            } else {
                System.out.println("❌ Utilisateur introuvable !");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 🔥 Vérification complète
    private static void verifyPassword(Connection con, int userId, String rawPassword) {

        String sql = "SELECT mot_de_passe FROM utilisateurs WHERE id_user = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String dbHash = rs.getString("mot_de_passe");

                System.out.println("🔎 Hash en base : " + dbHash);

                // 1. Vérifier format BCrypt
                if (!dbHash.startsWith("$2a$") && !dbHash.startsWith("$2b$")) {
                    System.out.println("❌ ERREUR : mot de passe non BCrypt !");
                    return;
                }

                // 2. Vérification BCrypt réelle
                if (BCrypt.checkpw(rawPassword, dbHash)) {
                    System.out.println("✔ Vérification OK : mot de passe valide");
                } else {
                    System.out.println("❌ ERREUR : mismatch password !");
                }

            } else {
                System.out.println("❌ Aucun utilisateur trouvé après update");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        updatePassword(1, "1234!");
    }
}