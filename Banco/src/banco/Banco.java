/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package banco;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.SwingUtilities;

/**
 *
 * @author davek
 */
public class Banco {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
        } catch(Exception e) {
            System.err.println("No se pudo cargar FlatLaf, usando diseño por defecto.");
        }

        
        SwingUtilities.invokeLater(() -> {
            
            BancoGUI frame = new BancoGUI();
            frame.setVisible(true);
        });
    }
    
}
