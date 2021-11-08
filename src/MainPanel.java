import java.awt.*;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class MainPanel extends JPanel {

    private static Font uiFont = new Font("宋体", Font.PLAIN, 21);

    private JTextField filePathField;
    private JPasswordField passwordField;
    private JCheckBox isPwdEchoCheck;
    private JButton encryptButton;
    private JButton decryptButton;
    private JLabel progressLabel;
    private JLabel usedTimeLabel;
    private JTextArea resultArea;

    // 计时器，用于更新已用时信息
    private Timer timer = null;
    // 计时器计时秒数
    private long timerSecondValue;

    private boolean isProcessing = false;

    public MainPanel() {
        // 禁用布局管理，直接绝对定位
        this.setLayout(null);
        initPanel();
        setActionListener();
    }

    private JLabel addLabel(String title) {
        JLabel label = new JLabel(title);
        this.add(label);
        label.setHorizontalAlignment(JLabel.RIGHT);
        return label;
    }

    private String getCurFilePath() {
        String curPath = System.getProperty("java.class.path");
        int firstIndex = curPath.lastIndexOf(System.getProperty("path.separator")) + 1;
        int lastIndex = curPath.lastIndexOf(File.separator) + 1;
        curPath = curPath.substring(firstIndex, lastIndex);
        return curPath;
    }

    private void initPanel() {
        UIManager.put("Label.font", uiFont);
        // 添加标签
        this.addLabel("文件路径： ").setBounds(40, 20, 120, 21);
        // 添加文件路径输入框
        filePathField = new JTextField();
        filePathField.setText(getCurFilePath());
        filePathField.setColumns(35);
        filePathField.setFont(uiFont);
        filePathField.setBounds(150, 18, 385, 26);
        this.add(filePathField);
        // 添加标签
        this.addLabel("密码： ").setBounds(40, 60, 120, 21);
        // 添加密码输入框
        // 默认显示输入的密码
        passwordField = new JPasswordField();
        passwordField.setEchoChar((char) 0);
        passwordField.setColumns(35);
        passwordField.setFont(uiFont);
        passwordField.setBounds(150, 58, 385, 26);
        this.add(passwordField);
        // 添加显示密码选择框
        isPwdEchoCheck = new JCheckBox("显示密码", true);
        isPwdEchoCheck.setFont(uiFont);
        isPwdEchoCheck.setBounds(150, 90, 130, 21);
        this.add(isPwdEchoCheck);
        // 添加加密按钮
        encryptButton = new JButton("加密");
        encryptButton.setFont(uiFont);
        encryptButton.setBounds(150, 120, 100, 32);
        this.add(encryptButton);
        // 添加解密按钮
        decryptButton = new JButton("解密");
        decryptButton.setFont(uiFont);
        decryptButton.setBounds(350, 120, 100, 32);
        this.add(decryptButton);
        // 添加标签
        this.addLabel("进度： ").setBounds(80, 165, 80, 21);
        // 添加进度显示标签
        progressLabel = new JLabel();
        progressLabel.setBounds(160, 165, 80, 21);
        this.add(progressLabel);
        // 添加标签
        this.addLabel("已用时： ").setBounds(250, 165, 100, 21);
        // 添加已用时标签
        usedTimeLabel = new JLabel();
        usedTimeLabel.setBounds(350, 165, 250, 21);
        this.add(usedTimeLabel);
        // 添加结果显示区域
        resultArea = new JTextArea();
        resultArea.setAutoscrolls(true);
        resultArea.setBorder(BorderFactory.createEtchedBorder());
        resultArea.setFont(uiFont);
        resultArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(75, 190, 450, 150);
        scrollPane.setViewportView(resultArea);
        this.add(scrollPane);
    }

    private void setActionListener() {
        // 添加选择框监听，切换密码是否显示
        isPwdEchoCheck.addChangeListener(e -> {
            if (isPwdEchoCheck.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('*');
            }
        });
        // 添加加密按钮监听
        encryptButton.addActionListener(e -> {
            if (isProcessing) {
                this.showMessage("当前正在执行其他操作");
                return;
            }
            String filePath = filePathField.getText();
            String password = new String(passwordField.getPassword());
            new Thread(() -> {
                CryptTool.encrypt(filePath, password, this);
            }).start();
        });
        // 添加解密按钮监听
        decryptButton.addActionListener(e -> {
            if (isProcessing) {
                this.showMessage("当前正在执行其他操作");
                return;
            }
            String filePath = filePathField.getText();
            String password = new String(passwordField.getPassword());
            new Thread(() -> {
                CryptTool.decrypt(filePath, password, this);
            }).start();
        });
    }

    public synchronized void appendResult(String result) {
        String text = resultArea.getText();
        if (text != null && !"".equals(text)) {
            text += '\n';
        }
        text += result;
        resultArea.setText(text);
    }

    public synchronized void updateProgress(String str) {
        progressLabel.setText(str);
    }

    public void startTimer() {
        timerSecondValue = 0;
        // 先清空计时器
        timer = new Timer();
        usedTimeLabel.setText("");
        timer.schedule(new TimerTask() {
            public void run() {
                ++timerSecondValue;
                usedTimeLabel.setText(BasicUtil.getTimeDescription(timerSecondValue));
            }
        }, 0, 1000);
    }

    public void endTimer() {
        if (timer == null) {
            return;
        }
        timer.cancel();
        timer.purge();
    }

    public void startProcess() {
        isProcessing = true;
    }

    public void endProcess() {
        isProcessing = false;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void showConfirm(int times, String message, Consumer<Boolean> fun) {
        doConfirm(1, times, message, fun);
    }

    private void doConfirm(int thisTime, int allTime, String message, Consumer<Boolean> fun) {
        String msg = message + "(" + thisTime + "/" + allTime + ")";
        int option = JOptionPane.showConfirmDialog(this, msg, "提示", JOptionPane.YES_NO_CANCEL_OPTION);
        if (option != JOptionPane.YES_OPTION) {
            fun.accept(false);
        } else {
            if (thisTime != allTime) {
                doConfirm(thisTime + 1, allTime, message, fun);
            } else {
                fun.accept(true);
            }
        }
    }
}
