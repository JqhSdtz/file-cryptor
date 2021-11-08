import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class MainFrame extends JFrame {

    private MainPanel mainPanel = new MainPanel();

    public MainFrame() {
        this.setTitle(AppConfig.getTitle());
        this.setBounds(0, 0, AppConfig.getWidth(), AppConfig.getHeight());
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setResizable(false);
        initFrame();
    }

    private void initFrame() {
        this.add(mainPanel);
        MainFrame mainFrame = this;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (e.getWindow() != mainFrame) {
                    return;
                }
                if (mainPanel.isProcessing()) {
                    mainPanel.showConfirm(3, "当前有正在执行的任务，强制退出可能造成数据损坏，是否继续？", isConfirm -> {
                        if (isConfirm) {
                            System.exit(0);
                        }
                    });
                } else {
                    System.exit(0);
                }
            }
        });
    }
}
