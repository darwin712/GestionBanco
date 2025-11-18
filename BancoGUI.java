import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class BancoGUI extends JFrame {

    // --- Componentes públicos para que el controlador los pueda usar ---
    public JTable tablaCuentas;
    public JTable tablaTransacciones;

    public JButton btnCrearCuenta;
    public JButton btnCerrarCuenta;
    public JButton btnModificarTitular;
    public JButton btnDepositar;
    public JButton btnRetirar;
    public JButton btnTransferir;
    public JButton btnBuscarCuenta;
    public JButton btnActualizarListas;

    public JTextField txtTitular;
    public JTextField txtMonto;
    public JTextField txtCuentaOrigen;
    public JTextField txtCuentaDestino;
    public JTextField txtBuscarID;

    public BancoGUI() {
        setTitle("Sistema Bancario - Interfaz Visual");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        // -------------------- PESTAÑA CUENTAS --------------------
        JPanel panelCuentas = new JPanel(new BorderLayout());

        tablaCuentas = new JTable(new DefaultTableModel(
            new Object[]{"ID", "Titular", "Saldo", "Estado"}, 0
        ));
        panelCuentas.add(new JScrollPane(tablaCuentas), BorderLayout.CENTER);

        JPanel panelSuperior = new JPanel(new FlowLayout());

        txtTitular = new JTextField(12);
        panelSuperior.add(new JLabel("Titular:"));
        panelSuperior.add(txtTitular);

        btnCrearCuenta = new JButton("Crear Cuenta");
        btnCerrarCuenta = new JButton("Cerrar Cuenta");
        btnModificarTitular = new JButton("Modificar Titular");

        panelSuperior.add(btnCrearCuenta);
        panelSuperior.add(btnCerrarCuenta);
        panelSuperior.add(btnModificarTitular);

        panelCuentas.add(panelSuperior, BorderLayout.NORTH);

        tabs.addTab("Cuentas", panelCuentas);

        // -------------------- PESTAÑA TRANSACCIONES --------------------
        JPanel panelTrans = new JPanel(new BorderLayout());

        tablaTransacciones = new JTable(new DefaultTableModel(
            new Object[]{"ID", "Tipo", "Monto", "Cuenta Origen", "Cuenta Destino", "Fecha"}, 0
        ));
        panelTrans.add(new JScrollPane(tablaTransacciones), BorderLayout.CENTER);

        JPanel panelAcciones = new JPanel(new GridLayout(3, 1));

        // --- Operaciones Monetarias ---
        JPanel panelMov = new JPanel(new FlowLayout());
        txtMonto = new JTextField(8);
        panelMov.add(new JLabel("Monto:"));
        panelMov.add(txtMonto);

        btnDepositar = new JButton("Depositar");
        btnRetirar = new JButton("Retirar");

        panelMov.add(btnDepositar);
        panelMov.add(btnRetirar);

        // --- Transferencias ---
        JPanel panelTransf = new JPanel(new FlowLayout());
        txtCuentaOrigen = new JTextField(6);
        txtCuentaDestino = new JTextField(6);

        panelTransf.add(new JLabel("Cuenta Origen:"));
        panelTransf.add(txtCuentaOrigen);

        panelTransf.add(new JLabel("Cuenta Destino:"));
        panelTransf.add(txtCuentaDestino);

        btnTransferir = new JButton("Transferir");
        panelTransf.add(btnTransferir);

        // --- Búsqueda ---
        JPanel panelBusqueda = new JPanel(new FlowLayout());
        txtBuscarID = new JTextField(6);
        btnBuscarCuenta = new JButton("Buscar");
        btnActualizarListas = new JButton("Actualizar listas");

        panelBusqueda.add(new JLabel("Buscar ID:"));
        panelBusqueda.add(txtBuscarID);
        panelBusqueda.add(btnBuscarCuenta);
        panelBusqueda.add(btnActualizarListas);

        panelAcciones.add(panelMov);
        panelAcciones.add(panelTransf);
        panelAcciones.add(panelBusqueda);

        panelTrans.add(panelAcciones, BorderLayout.NORTH);

        tabs.addTab("Transacciones", panelTrans);

        add(tabs);
    }

    // --- MAIN SOLO PARA PROBAR LA INTERFAZ ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BancoGUI().setVisible(true));
    }
}

