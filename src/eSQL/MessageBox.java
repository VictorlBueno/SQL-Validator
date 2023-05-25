package eSQL;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MessageBox {
    private JFrame window;
    private JLabel messageLabel;

    public MessageBox() {
        // Criar uma instância da janela
        window = new JFrame("SQL Errors");

        // Criar um rótulo com a mensagem inicial "Carregando..."
        messageLabel = new JLabel("Carregando...");

        // Criar um painel com barra de rolagem e adicionar o rótulo a ele
        JScrollPane scrollPane = new JScrollPane(messageLabel);

        // Adicionar o painel de rolagem à janela
        window.getContentPane().add(scrollPane);

        // Configurar o tamanho da janela
        window.setSize(500, 200);

        // Definir a ação padrão ao fechar a janela
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Adicionar um WindowListener para tratar o evento de fechamento da janela
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        // Exibir a janela após um pequeno intervalo para simular o carregamento
        SwingUtilities.invokeLater(() -> {
            window.setVisible(true);
        });
    }

    public void loading(int count, int errors) {
        // Atualizar a mensagem no rótulo
        String text = "<html><h3>&nbsp;&nbsp;&nbsp;Analisando...</h3><small>&nbsp;&nbsp;&nbsp;&nbsp;Outros programas em execução podem afetar a velocidade de processamento</small><br><br>&nbsp;&nbsp;&nbsp;&nbsp;" + count + " Códigos Verificados<br>&nbsp;&nbsp;&nbsp;&nbsp;" + errors + " Erros encontrados</html>";
        messageLabel.setText(text);
    }

    public void showDetails(String text, int counter) {
        // Remover todos os componentes da janela
        window.getContentPane().removeAll();

        text = "<html><h3>&nbsp;&nbsp;&nbsp;Resultado:</h3>&nbsp;&nbsp;&nbsp;&nbsp;"+ counter + " Erros Encontrados<br>&nbsp;&nbsp;&nbsp;&nbsp;------------------------------<br>" + text + "</html>";

        // Criar um rótulo com os detalhes
        JLabel detailsLabel = new JLabel(text);

        // Criar um painel com barra de rolagem e adicionar o rótulo a ele
        JScrollPane scrollPane = new JScrollPane(detailsLabel);

        // Adicionar o painel de rolagem à janela
        window.getContentPane().add(scrollPane);

        // Configurar o tamanho da janela
        window.setSize(700, 300);
    }

    public void dispose() {
        window.dispose();
    }

    public JFrame getWindow() {
        return window;
    }
}
