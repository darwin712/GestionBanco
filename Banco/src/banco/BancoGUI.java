
package banco;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 *
 * @author davek
 */
public class BancoGUI extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(BancoGUI.class.getName());

    private Cuenta[] cuentas = new Cuenta[10];
    private int contadorCuentas = 0;

    // REQ I: Máximo 20 transacciones activas
    private Transaccion[] transActivas = new Transaccion[20];
    private int contTransActivas = 0;

    // REQ J: Máximo 10 transacciones desactivadas (historial de cuentas cerradas)
    private Transaccion[] transInactivas = new Transaccion[10];
    private int contTransInactivas = 0;

    // Modelos de tabla
    private DefaultTableModel modeloCuentas;
    private DefaultTableModel modeloTransacciones;
    /**
     * Creates new form BancoGUI
     */
    public BancoGUI() {
        initComponents();
        
        configurarTablas();
        asignarLogicaBotones();
        this.setTitle("Sistema Bancario - Proyecto Final (40%)");
        this.setLocationRelativeTo(null);
    }

    // --- CLASES MODELO ---
    class Cuenta {
        int id;
        String titular;
        double saldo;
        boolean activa;

        public Cuenta(int id, String titular, double saldo) {
            this.id = id;
            this.titular = titular;
            this.saldo = saldo;
            this.activa = true;
        }
    }

    class Transaccion {
        int id;
        String tipo;
        double monto;
        int idOrigen;
        int idDestino;
        String fecha;

        public Transaccion(int id, String tipo, double monto, int idOrigen, int idDestino) {
            this.id = id;
            this.tipo = tipo;
            this.monto = monto;
            this.idOrigen = idOrigen;
            this.idDestino = idDestino;
            this.fecha = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date());
        }
    }

    // --- CONFIGURACIÓN ---
    private void configurarTablas() {
        // REQ H: Tabla de cuentas
        modeloCuentas = new DefaultTableModel();
        modeloCuentas.addColumn("ID");
        modeloCuentas.addColumn("Titular");
        modeloCuentas.addColumn("Saldo");
        modeloCuentas.addColumn("Estado");
        jTableCuentas.setModel(modeloCuentas);

        // REQ I y J: Tabla de transacciones
        modeloTransacciones = new DefaultTableModel();
        modeloTransacciones.addColumn("ID");
        modeloTransacciones.addColumn("Tipo");
        modeloTransacciones.addColumn("Monto");
        modeloTransacciones.addColumn("Origen");
        modeloTransacciones.addColumn("Destino");
        modeloTransacciones.addColumn("Fecha");
        jTable2.setModel(modeloTransacciones);
    }

    private void asignarLogicaBotones() {
        btnCrear.addActionListener(e -> crearCuenta());
        btnCerrar.addActionListener(e -> cerrarCuenta());
        btnModificar.addActionListener(e -> modificarCuenta());
        btnDepositar.addActionListener(e -> depositar());
        btnRetirar.addActionListener(e -> retirar());
        btnTransferir.addActionListener(e -> transferir());
        btnBuscar.addActionListener(e -> buscarCuentaPorID());
        // Modificado para cumplir REQ J (Consultar historial desactivado)
        btnFiltrar.addActionListener(e -> consultarTransacciones()); 
    }

    // --- LÓGICA DE NEGOCIO ---

    private void crearCuenta() {
        if (contadorCuentas >= cuentas.length) {
            mostrarError("Límite de cuentas alcanzado (Máximo 10).");
            return;
        }

        String titular = JOptionPane.showInputDialog(this, "Ingrese el nombre del Titular:");
        if (titular == null || titular.trim().isEmpty()) {
            mostrarError("El nombre del titular es obligatorio.");
            return;
        }

        String saldoStr = JOptionPane.showInputDialog(this, "Ingrese saldo inicial para " + titular + ":");
        try {
            double saldo = Double.parseDouble(saldoStr);
            if (saldo < 0) {
                mostrarError("El saldo no puede ser negativo.");
                return;
            }

            int nuevoID = (int) (Math.random() * 9000) + 1000; // Generar ID aleatorio
            cuentas[contadorCuentas] = new Cuenta(nuevoID, titular, saldo);
            contadorCuentas++;
            
            actualizarTablaCuentas();
            JOptionPane.showMessageDialog(this, "Cuenta creada exitosamente.\nID Generado: " + nuevoID);

        } catch (NumberFormatException | NullPointerException e) {
            mostrarError("Dato inválido. Ingrese un número para el saldo.");
        }
    }

    // 2. Cerrar Cuenta
    private void cerrarCuenta() {
        // Intentar obtener ID de la selección de la tabla primero
        int fila = jTableCuentas.getSelectedRow();
        String idStr = null;

        if (fila != -1) {
            idStr = jTableCuentas.getValueAt(fila, 0).toString();
        } else {
            // Si no seleccionó, pedir por input
            idStr = JOptionPane.showInputDialog(this, "Ingrese ID de la cuenta a cerrar:");
        }

        if (idStr == null) return;

        try {
            int id = Integer.parseInt(idStr);
            int idx = buscarIndicePorID(id);

            if (idx == -1) {
                mostrarError("Cuenta no encontrada.");
                return;
            }

            if (!cuentas[idx].activa) {
                mostrarError("Esta cuenta ya está cerrada.");
                return;
            }

            if (cuentas[idx].saldo > 0) {
                mostrarError("No se puede cerrar. La cuenta debe estar en ceros.\nSaldo actual: " + cuentas[idx].saldo);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, 
                "Información de cuenta a cerrar:\nID: " + cuentas[idx].id + "\nTitular: " + cuentas[idx].titular + "\n\n¿Confirma el cierre?",
                "Confirmar Cierre", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                cuentas[idx].activa = false;
                moverTransaccionesAHistorialInactivo(id);
                actualizarTablaCuentas();
                actualizarTablaTransacciones(true); // Mostrar activas por defecto
                JOptionPane.showMessageDialog(this, "Cuenta cerrada correctamente.");
            }

        } catch (NumberFormatException e) {
            mostrarError("ID inválido.");
        }
    }

    // 3. Modificar Titular
    private void modificarCuenta() {
        int fila = jTableCuentas.getSelectedRow();
        String idStr;

        if (fila != -1) {
            idStr = jTableCuentas.getValueAt(fila, 0).toString();
        } else {
            idStr = JOptionPane.showInputDialog(this, "Ingrese ID de la cuenta a modificar:");
        }

        if (idStr == null) return;

        try {
            int id = Integer.parseInt(idStr);
            int idx = buscarIndicePorID(id);

            if (idx == -1) {
                mostrarError("Cuenta no encontrada.");
                return;
            }

            String nuevoNombre = JOptionPane.showInputDialog(this, "Modificar nombre del titular (" + cuentas[idx].titular + "):", cuentas[idx].titular);
            if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) {
                cuentas[idx].titular = nuevoNombre;
                actualizarTablaCuentas();
                JOptionPane.showMessageDialog(this, "Nombre actualizado.");
            }
        } catch (NumberFormatException e) {
            mostrarError("ID inválido.");
        }
    }

    // 4. Depósito
    private void depositar() {
        try {
            String idStr = JOptionPane.showInputDialog(this, "Ingrese ID Cuenta Destino:");
            if (idStr == null) return;
            int id = Integer.parseInt(idStr);

            String montoStr = JOptionPane.showInputDialog(this, "Ingrese monto a depositar:");
            if (montoStr == null) return;
            double monto = Double.parseDouble(montoStr);

            registrarTransaccion("Depósito", monto, 0, id);
            JOptionPane.showMessageDialog(this, "Depósito realizado.");

        } catch (Exception e) {
            mostrarError("Datos inválidos (Monto o ID) o cuenta inexistente.");
        }
    }

    // 5. Retiro
    private void retirar() {
        try {
            String idStr = JOptionPane.showInputDialog(this, "Ingrese ID Cuenta Origen:");
            if (idStr == null) return;
            int id = Integer.parseInt(idStr);

            String montoStr = JOptionPane.showInputDialog(this, "Ingrese monto a retirar:");
            if (montoStr == null) return;
            double monto = Double.parseDouble(montoStr);

            registrarTransaccion("Retiro", monto, id, 0);
            JOptionPane.showMessageDialog(this, "Retiro realizado.");

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    // 6. Transferencia
    private void transferir() {
        try {
            String idOrStr = JOptionPane.showInputDialog(this, "Ingrese ID Cuenta Origen:");
            if (idOrStr == null) return;
            int idOr = Integer.parseInt(idOrStr);

            String idDeStr = JOptionPane.showInputDialog(this, "Ingrese ID Cuenta Destino:");
            if (idDeStr == null) return;
            int idDe = Integer.parseInt(idDeStr);

            String montoStr = JOptionPane.showInputDialog(this, "Ingrese monto a transferir:");
            if (montoStr == null) return;
            double monto = Double.parseDouble(montoStr);

            registrarTransaccion("Transferencia", monto, idOr, idDe);
            JOptionPane.showMessageDialog(this, "Transferencia exitosa.");

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    // 7. Buscar por ID (Auxiliar)
    private void buscarCuentaPorID() {
        String idStr = JOptionPane.showInputDialog(this, "Ingrese ID a buscar:");
        if (idStr != null) {
            try {
                int id = Integer.parseInt(idStr);
                int idx = buscarIndicePorID(id);
                if (idx != -1) {
                    // Seleccionar en la tabla
                    for (int i = 0; i < jTableCuentas.getRowCount(); i++) {
                        if (Integer.parseInt(jTableCuentas.getValueAt(i, 0).toString()) == id) {
                            jTableCuentas.setRowSelectionInterval(i, i);
                            jTableCuentas.scrollRectToVisible(jTableCuentas.getCellRect(i, 0, true));
                            JOptionPane.showMessageDialog(this, "Cuenta encontrada: " + cuentas[idx].titular);
                            return;
                        }
                    }
                } else {
                    mostrarError("Cuenta no encontrada.");
                }
            } catch (Exception e) {
                mostrarError("ID inválido.");
            }
        }
    }

    // 8. Filtrar Transacciones (Antes "Actualizar")
    private void consultarTransacciones() {
        String[] opciones = {"Ver Activas", "Ver Cerradas (Historial)"};
        int seleccion = JOptionPane.showOptionDialog(this,
                "¿Qué lista de transacciones desea consultar?",
                "Selector de Historial",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);

        if (seleccion == 0) {
            actualizarTablaTransacciones(true); // Activas
        } else if (seleccion == 1) {
            actualizarTablaTransacciones(false); // Inactivas
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private void registrarTransaccion(String tipo, double monto, int idOrigen, int idDestino) throws Exception {
        if (monto <= 0) throw new Exception("El monto debe ser positivo.");

        int idxOr = -1;
        int idxDe = -1;

        // Validaciones y lógica de saldos
        if (idOrigen != 0) {
            idxOr = buscarIndicePorID(idOrigen);
            if (idxOr == -1 || !cuentas[idxOr].activa) throw new Exception("Cuenta origen inválida o cerrada.");
            if (cuentas[idxOr].saldo < monto) throw new Exception("Saldo insuficiente.");
        }

        if (idDestino != 0) {
            idxDe = buscarIndicePorID(idDestino);
            if (idxDe == -1 || !cuentas[idxDe].activa) throw new Exception("Cuenta destino inválida o cerrada.");
        }

        // Ejecutar movimiento
        if (idxOr != -1) cuentas[idxOr].saldo -= monto;
        if (idxDe != -1) cuentas[idxDe].saldo += monto;

        // Guardar transacción
        int idTrans = 10000 + contTransActivas;
        Transaccion t = new Transaccion(idTrans, tipo, monto, idOrigen, idDestino);
        
        if (contTransActivas < transActivas.length) {
            transActivas[contTransActivas] = t;
            contTransActivas++;
        }

        actualizarTablaCuentas();
        actualizarTablaTransacciones(true);
    }

    private int buscarIndicePorID(int id) {
        for (int i = 0; i < contadorCuentas; i++) {
            if (cuentas[i].id == id) return i;
        }
        return -1;
    }

    private void moverTransaccionesAHistorialInactivo(int idCuentaCerrada) {
        // Mover transacciones asociadas a la cuenta cerrada de Activas a Inactivas
        // Nota: Esta es una simplificación. En un sistema real usaríamos listas dinámicas (ArrayList).
        
        Transaccion[] tempActivas = new Transaccion[100];
        int tempCount = 0;

        for (int i = 0; i < contTransActivas; i++) {
            Transaccion t = transActivas[i];
            if (t.idOrigen == idCuentaCerrada || t.idDestino == idCuentaCerrada) {
                // Mover a inactivas
                if (contTransInactivas < transInactivas.length) {
                    transInactivas[contTransInactivas] = t;
                    contTransInactivas++;
                }
            } else {
                // Mantener en activas
                tempActivas[tempCount] = t;
                tempCount++;
            }
        }
        transActivas = tempActivas;
        contTransActivas = tempCount;
    }

    private void actualizarTablaCuentas() {
        modeloCuentas.setRowCount(0);
        for (int i = 0; i < contadorCuentas; i++) {
            Cuenta c = cuentas[i];
            modeloCuentas.addRow(new Object[]{
                c.id, 
                c.titular, 
                c.saldo, 
                c.activa ? "Activa" : "Cerrada"
            });
        }
    }

    private void actualizarTablaTransacciones(boolean mostrarActivas) {
        modeloTransacciones.setRowCount(0);
        Transaccion[] fuente = mostrarActivas ? transActivas : transInactivas;
        int cantidad = mostrarActivas ? contTransActivas : contTransInactivas;

        for (int i = 0; i < cantidad; i++) {
            Transaccion t = fuente[i];
            modeloTransacciones.addRow(new Object[]{
                t.tipo,
                t.monto,
                t.idOrigen == 0 ? "-" : t.idOrigen,
                t.idDestino == 0 ? "-" : t.idDestino,
                t.fecha
            });
        }
    }
    
    

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error / Validación", JOptionPane.ERROR_MESSAGE);
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableCuentas = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        btnCrear = new javax.swing.JButton();
        btnCerrar = new javax.swing.JButton();
        btnModificar = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        btnDepositar = new javax.swing.JButton();
        btnRetirar = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        btnTransferir = new javax.swing.JButton();
        btnFiltrar = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jTableCuentas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID", "Titular", "Saldo", "Estado"
            }
        ));
        jScrollPane1.setViewportView(jTableCuentas);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        btnCrear.setText("Crear cuenta");

        btnCerrar.setText("Cerrar cuenta");

        btnModificar.setText("Modificar titular");

        btnBuscar.setText("Buscar");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(88, 88, 88)
                .addComponent(btnCrear, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCerrar, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnModificar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(81, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCrear)
                    .addComponent(btnCerrar)
                    .addComponent(btnModificar)
                    .addComponent(btnBuscar))
                .addContainerGap(40, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel3, java.awt.BorderLayout.PAGE_START);

        jTabbedPane1.addTab("Cuentas", jPanel1);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Tipo", "Monto", "Cuenta origen", "Cuenta destino", "Fecha"
            }
        ));
        jScrollPane2.setViewportView(jTable2);

        jPanel2.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jPanel4.setLayout(new java.awt.GridLayout(3, 1));

        btnDepositar.setText("Depositar");

        btnRetirar.setText("Retirar");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(193, 193, 193)
                .addComponent(btnDepositar)
                .addGap(77, 77, 77)
                .addComponent(btnRetirar)
                .addContainerGap(214, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDepositar)
                    .addComponent(btnRetirar))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel5);

        btnTransferir.setText("Transferir");

        btnFiltrar.setText("Filtrar");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(192, 192, 192)
                .addComponent(btnTransferir)
                .addGap(76, 76, 76)
                .addComponent(btnFiltrar)
                .addContainerGap(216, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnTransferir)
                    .addComponent(btnFiltrar))
                .addContainerGap())
        );

        jPanel4.add(jPanel6);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 636, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 35, Short.MAX_VALUE)
        );

        jPanel4.add(jPanel7);

        jPanel2.add(jPanel4, java.awt.BorderLayout.PAGE_START);

        jTabbedPane1.addTab("Transacciones", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 510, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new BancoGUI().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCerrar;
    private javax.swing.JButton btnCrear;
    private javax.swing.JButton btnDepositar;
    private javax.swing.JButton btnFiltrar;
    private javax.swing.JButton btnModificar;
    private javax.swing.JButton btnRetirar;
    private javax.swing.JButton btnTransferir;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTableCuentas;
    // End of variables declaration//GEN-END:variables
}
