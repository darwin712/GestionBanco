/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
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
        jTable1.setModel(modeloCuentas);

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
        jButton1.addActionListener(e -> crearCuenta());
        jButton2.addActionListener(e -> cerrarCuenta());
        jButton3.addActionListener(e -> modificarCuenta());
        jButton4.addActionListener(e -> depositar());
        jButton5.addActionListener(e -> retirar());
        jButton6.addActionListener(e -> transferir());
        jButton7.addActionListener(e -> buscarCuentaPorID());
        // Modificado para cumplir REQ J (Consultar historial desactivado)
        jButton8.addActionListener(e -> consultarTransacciones()); 
    }

    // --- LÓGICA DE NEGOCIO ---

    // REQ A: Crear cuenta (Max 10, ID auto, Saldo input, No repetir ID)
    private void crearCuenta() {
        if (contadorCuentas >= 10) {
            mostrarError("Límite de cuentas alcanzado (Máximo 10).");
            return;
        }

        String titular = jTextField1.getText().trim();
        if (titular.isEmpty()) {
            mostrarError("El nombre del titular es obligatorio.");
            return;
        }

        try {
            String saldoStr = JOptionPane.showInputDialog(this, "Ingrese saldo inicial para " + titular + ":");
            if (saldoStr == null) return;
            double saldo = Double.parseDouble(saldoStr);
            if (saldo < 0) {
                mostrarError("El saldo no puede ser negativo.");
                return;
            }

            // Generación automática de ID y validación de no repetición
            int nuevoID = 1001 + contadorCuentas; 
            // Verificación paranoica de ID único
            while (buscarIndicePorID(nuevoID) != -1) {
                nuevoID++;
            }

            cuentas[contadorCuentas] = new Cuenta(nuevoID, titular, saldo);
            contadorCuentas++;
            
            actualizarTablaCuentas();
            jTextField1.setText("");
            JOptionPane.showMessageDialog(this, "Cuenta creada exitosamente.\nID Generado: " + nuevoID);

        } catch (NumberFormatException e) {
            mostrarError("Dato inválido. Ingrese un número para el saldo.");
        }
    }

    // REQ B: Cerrar cuenta (Mostrar info, Confirmar, Solo si saldo es 0)
    private void cerrarCuenta() {
        int fila = jTable1.getSelectedRow();
        if (fila == -1) {
            mostrarError("Seleccione una cuenta de la tabla para cerrar.");
            return;
        }

        int id = (int) jTable1.getValueAt(fila, 0);
        int idx = buscarIndicePorID(id);
        Cuenta c = cuentas[idx];

        if (!c.activa) {
            mostrarError("Esta cuenta ya está cerrada.");
            return;
        }

        // Mostrar información antes de eliminar
        String info = "Información de cuenta a cerrar:\n" +
                      "ID: " + c.id + "\n" +
                      "Titular: " + c.titular + "\n" +
                      "Saldo: $" + c.saldo;
        
        int confirm = JOptionPane.showConfirmDialog(this, info + "\n\n¿Confirma el cierre?", "Confirmar Cierre", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Validación saldo cero
        if (c.saldo != 0.0) {
            mostrarError("No se puede cerrar. La cuenta debe estar en ceros.\nSaldo actual: " + c.saldo);
            return;
        }

        // Proceder al cierre
        c.activa = false;
        
        // Mover transacciones al historial inactivo (REQ I y J)
        moverTransaccionesAHistorialInactivo(c.id);
        
        actualizarTablaCuentas();
        actualizarTablaTransacciones(true); // Refrescar vista activas
        JOptionPane.showMessageDialog(this, "Cuenta cerrada correctamente.");
    }

    // REQ C: Modificar cuenta (Solo nombre, pedir confirmación)
    private void modificarCuenta() {
        int fila = jTable1.getSelectedRow();
        if (fila == -1) {
            mostrarError("Seleccione una cuenta para modificar.");
            return;
        }

        int id = (int) jTable1.getValueAt(fila, 0);
        Cuenta c = cuentas[buscarIndicePorID(id)];

        String nuevoNombre = JOptionPane.showInputDialog(this, "Modificar nombre del titular:", c.titular);
        if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this, "¿Seguro desea cambiar el nombre a: " + nuevoNombre + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                c.titular = nuevoNombre;
                actualizarTablaCuentas();
            }
        }
    }

    // REQ D: Depositar (Cuenta existente)
    private void depositar() {
        try {
            int id = obtenerInt(JOptionPane.showInputDialog(this, "Ingrese ID Cuenta Destino:"));
            if (id == -1) return;
            
            int idx = buscarIndicePorID(id);
            if (idx == -1 || !cuentas[idx].activa) {
                mostrarError("Cuenta no existe o está cerrada.");
                return;
            }

            double monto = Double.parseDouble(jTextField2.getText());
            if (monto <= 0) throw new NumberFormatException();

            cuentas[idx].saldo += monto;
            registrarTransaccion("Depósito", monto, id, id);
            
            actualizarTablaCuentas();
            jTextField2.setText("");
            JOptionPane.showMessageDialog(this, "Depósito realizado.");

        } catch (Exception e) {
            mostrarError("Datos inválidos (Monto o ID).");
        }
    }

    // REQ E: Retirar (Verificar saldo)
    private void retirar() {
        try {
            int id = obtenerInt(JOptionPane.showInputDialog(this, "Ingrese ID Cuenta Origen:"));
            if (id == -1) return;

            int idx = buscarIndicePorID(id);
            if (idx == -1 || !cuentas[idx].activa) {
                mostrarError("Cuenta no existe o está cerrada.");
                return;
            }

            double monto = Double.parseDouble(jTextField2.getText());
            if (monto <= 0) throw new NumberFormatException();

            if (cuentas[idx].saldo >= monto) {
                cuentas[idx].saldo -= monto;
                registrarTransaccion("Retiro", monto, id, id);
                actualizarTablaCuentas();
                jTextField2.setText("");
                JOptionPane.showMessageDialog(this, "Retiro realizado.");
            } else {
                mostrarError("Saldo insuficiente.");
            }
        } catch (Exception e) {
            mostrarError("Datos inválidos.");
        }
    }

    // REQ F: Transferir (Verificar saldo origen y existencia destino)
    private void transferir() {
        try {
            int idOrigen = Integer.parseInt(jTextField3.getText());
            int idDestino = Integer.parseInt(jTextField4.getText());
            
            // Usamos el campo de monto principal o pedimos uno nuevo
            String montoStr = jTextField2.getText();
            if (montoStr.isEmpty()) montoStr = JOptionPane.showInputDialog("Ingrese monto a transferir:");
            double monto = Double.parseDouble(montoStr);

            int idxOr = buscarIndicePorID(idOrigen);
            int idxDe = buscarIndicePorID(idDestino);

            if (idxOr == -1 || !cuentas[idxOr].activa) { mostrarError("Cuenta origen inválida."); return; }
            if (idxDe == -1 || !cuentas[idxDe].activa) { mostrarError("Cuenta destino inválida."); return; }

            if (cuentas[idxOr].saldo >= monto) {
                cuentas[idxOr].saldo -= monto;
                cuentas[idxDe].saldo += monto;
                registrarTransaccion("Transferencia", monto, idOrigen, idDestino);
                
                actualizarTablaCuentas();
                jTextField3.setText(""); jTextField4.setText(""); jTextField2.setText("");
                JOptionPane.showMessageDialog(this, "Transferencia exitosa.");
            } else {
                mostrarError("Saldo insuficiente en cuenta origen.");
            }
        } catch (Exception e) {
            mostrarError("Verifique todos los campos numéricos.");
        }
    }

    // REQ G: Buscar Cuenta por ID (Tabla)
    private void buscarCuentaPorID() {
        try {
            int id = Integer.parseInt(jTextField5.getText());
            modeloCuentas.setRowCount(0); // Limpiar tabla
            
            int idx = buscarIndicePorID(id);
            if (idx != -1) {
                Cuenta c = cuentas[idx];
                modeloCuentas.addRow(new Object[]{c.id, c.titular, c.saldo, c.activa ? "Activa" : "Cerrada"});
            } else {
                mostrarError("Cuenta no encontrada.");
            }
        } catch (Exception e) {
            mostrarError("ID inválido.");
        }
    }

    // REQ H y J: Consultar cuentas / Transacciones
    // Modifiqué este botón para que sirva de selector de historiales
    private void consultarTransacciones() {
        String[] opciones = {"Ver Activas", "Ver Cerradas (Historial)"};
        int seleccion = JOptionPane.showOptionDialog(this, 
                "¿Qué lista de transacciones desea consultar?", 
                "Selector de Historial", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);

        if (seleccion == 0) {
            actualizarTablaTransacciones(true); // Activas
            actualizarTablaCuentas(); // Refrescar cuentas también
        } else if (seleccion == 1) {
            actualizarTablaTransacciones(false); // Cerradas
        }
    }

    // --- GESTIÓN DE TRANSACCIONES (El nucleo de REQ I y J) ---

    private void registrarTransaccion(String tipo, double monto, int or, int des) {
        // Desplazamiento FIFO si se llena el array de activas (Max 20)
        if (contTransActivas >= 20) {
            for (int i = 0; i < 19; i++) {
                transActivas[i] = transActivas[i+1];
            }
            contTransActivas = 19;
        }
        
        int idTrans = 1 + contTransActivas + contTransInactivas + (int)(Math.random()*1000);
        transActivas[contTransActivas] = new Transaccion(idTrans, tipo, monto, or, des);
        contTransActivas++;
        
        actualizarTablaTransacciones(true);
    }

    private void moverTransaccionesAHistorialInactivo(int idCuentaCerrada) {
        // Creamos un array temporal para mantener las que sigan activas
        Transaccion[] tempActivas = new Transaccion[20];
        int tempCount = 0;

        for (int i = 0; i < contTransActivas; i++) {
            Transaccion t = transActivas[i];
            // Si la transacción pertenece a la cuenta cerrada
            if (t.idOrigen == idCuentaCerrada || t.idDestino == idCuentaCerrada) {
                // Mover a inactivas (Max 10 - FIFO)
                if (contTransInactivas < 10) {
                    transInactivas[contTransInactivas] = t;
                    contTransInactivas++;
                } else {
                    // Si historial desactivado lleno, borrar la más vieja y agregar nueva
                    for (int k = 0; k < 9; k++) transInactivas[k] = transInactivas[k+1];
                    transInactivas[9] = t;
                }
            } else {
                // Si no tiene que ver con la cerrada, se queda en activas
                tempActivas[tempCount] = t;
                tempCount++;
            }
        }
        // Reemplazar el array de activas con el filtrado
        transActivas = tempActivas;
        contTransActivas = tempCount;
    }

    // --- UTILIDADES VISUALES ---

    private void actualizarTablaCuentas() {
        modeloCuentas.setRowCount(0);
        for (int i = 0; i < contadorCuentas; i++) {
            Cuenta c = cuentas[i];
            modeloCuentas.addRow(new Object[]{c.id, c.titular, c.saldo, c.activa ? "Activa" : "Cerrada"});
        }
    }

    private void actualizarTablaTransacciones(boolean mostrarActivas) {
        modeloTransacciones.setRowCount(0);
        if (mostrarActivas) {
            // REQ I: Solo activas
            for (int i = 0; i < contTransActivas; i++) {
                Transaccion t = transActivas[i];
                modeloTransacciones.addRow(new Object[]{t.id, t.tipo, t.monto, t.idOrigen, t.idDestino, t.fecha});
            }
        } else {
            // REQ J: Solo desactivadas
            for (int i = 0; i < contTransInactivas; i++) {
                Transaccion t = transInactivas[i];
                modeloTransacciones.addRow(new Object[]{t.id, t.tipo + " (OFF)", t.monto, t.idOrigen, t.idDestino, t.fecha});
            }
        }
    }

    private int buscarIndicePorID(int id) {
        for (int i = 0; i < contadorCuentas; i++) {
            if (cuentas[i].id == id) return i;
        }
        return -1;
    }
    
    private int obtenerInt(String s) {
        try { return Integer.parseInt(s); } catch(Exception e) { return -1; }
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
        jTable1 = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jButton7 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jButton6 = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jButton8 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(jTable1);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jLabel1.setText("Titular");

        jButton1.setText("Crear cuenta");

        jButton2.setText("Cerrar cuenta");

        jButton3.setText("Modificar titular");

        jLabel5.setText("Buscar ID:");

        jButton7.setText("Buscar");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton3))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(201, 201, 201)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(jButton7)))
                .addContainerGap(32, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton7))
                .addContainerGap(11, Short.MAX_VALUE))
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

        jLabel2.setText("Monto:");

        jButton4.setText("Depositar");

        jButton5.setText("Retirar");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(150, 150, 150)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton5)
                .addContainerGap(168, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4)
                    .addComponent(jButton5))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel5);

        jLabel3.setText("Cuenta origen:");

        jLabel4.setText("Cuenta destino:");

        jButton6.setText("Transferir");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(111, 111, 111)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jButton6)
                .addContainerGap(103, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton6))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.add(jPanel6);

        jButton8.setText("Actualizar listas");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(253, 253, 253)
                .addComponent(jButton8)
                .addContainerGap(272, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton8)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
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
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    // End of variables declaration//GEN-END:variables
}
