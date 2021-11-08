import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CryptTool {

    private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(128, 1024, 10, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1024));

    private static final int blockSize = 1024 * 10;

    private static final int ENCRYPT = 1;
    private static final int DECRYPT = 2;

    private static final String fileSuffix = "hpny567";
    private static final String directorySuffix = "hpny234";
    private static final String nameStorageSuffix = "name";

    // 当前任务的总处理量
    private static long curTotalListedFileSize = 0L;
    // 当前任务的总处理文件数
    private static int curTotalListedFileNum = 0;
    // 当前任务的已处理量
    private static AtomicLong curTotalFinishedFileSize = new AtomicLong(0L);
    // 当前任务的已处理文件数
    private static AtomicInteger curTotalFinishedFileNum = new AtomicInteger(0);

    public static void encrypt(String path, String password, MainPanel mainPanel) {
        doCrypt(ENCRYPT, path, password, mainPanel);
    }

    public static void decrypt(String path, String password, MainPanel mainPanel) {
        doCrypt(DECRYPT, path, password, mainPanel);
    }

    private static void doCrypt(int mode, String path, String password, MainPanel mainPanel) {
        final String encryptedPassword = CryptUtil.md5(password);
        try {
            File root = new File(path);
            if (!root.exists()) {
                mainPanel.appendResult("不存在指定的文件或文件夹");
                return;
            }
            List<File> fileList = new ArrayList<>();
            searchDirectory(mode, fileList, root);
            long tmpFileSizeSum = 0L;
            for (int i = 0; i < fileList.size(); ++i) {
                tmpFileSizeSum += fileList.get(i).length();
            }
            String modeStr = getModeStr(mode);
            String msg = "选定待" + modeStr + "文件" + fileList.size() + "个，共"
                    + BasicUtil.getFileSizeDescription(tmpFileSizeSum) + "。" + modeStr + "过程不可取消，否则可能造成文件损坏。是否继续？";
            mainPanel.showConfirm(3, msg, isConfirm -> {
                if (isConfirm) {
                    curTotalListedFileSize = 0;
                    curTotalFinishedFileSize.set(0);
                    curTotalListedFileNum = fileList.size();
                    curTotalFinishedFileNum.set(0);
                    mainPanel.updateProgress("0%");
                    for (int i = 0; i < fileList.size(); ++i) {
                        long length = fileList.get(i).length();
                        // 这里的总长度是需要加密或解密的长度，所以需要补全后的长度
                        curTotalListedFileSize += length;
                        if (mode == ENCRYPT) {
                            curTotalListedFileSize += getFileLengthRemain(length);
                        } else if (mode == DECRYPT) {
                            // 文件尾不会超过blcokSize的长度，所以需要解密的长度就是原长度去掉除以blcokSize的余数
                            curTotalListedFileSize -= length % blockSize;
                        }
                    }
                    mainPanel.startProcess();
                    mainPanel.startTimer();
                    try {
                        // 因为文件夹名称加密后文件的路径已经改变，所以重新获取待加密文件
                        fileList.clear();
                        cryptDirectory(mode, encryptedPassword, fileList, root, mainPanel);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        mainPanel.appendResult(e1.getLocalizedMessage());
                    }
                    // 没有要加密的文件，则直接结束
                    if (fileList.size() == 0) {
                        onAllFileFinish(mode, mainPanel);
                    }
                    for (int i = 0; i < fileList.size(); ++i) {
                        final File file = fileList.get(i);
                        threadPool.execute(() -> {
                            try {
                                if (mode == ENCRYPT) {
                                    encryptSingleFile(file, encryptedPassword, mainPanel);
                                } else if (mode == DECRYPT) {
                                    decryptSingleFile(file, encryptedPassword, mainPanel);
                                }
                                onSingleFileFinish(mode, mainPanel);
                            } catch (IOException e) {
                                e.printStackTrace();
                                mainPanel.appendResult(e.getLocalizedMessage());
                            }
                        });
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            mainPanel.appendResult(e.getLocalizedMessage());
        }
    }

    private static String getModeStr(int mode) {
        String modeStr = null;
        if (mode == ENCRYPT) {
            modeStr = "加密";
        } else if (mode == DECRYPT) {
            modeStr = "解密";
        }
        return modeStr;
    }

    private static void onSingleFileFinish(int mode, MainPanel mainPanel) {
        int tmp = curTotalFinishedFileNum.incrementAndGet();
        if (tmp == curTotalListedFileNum) {
            onAllFileFinish(mode, mainPanel);
        }
    }

    private static void onAllFileFinish(int mode, MainPanel mainPanel) {
        mainPanel.endTimer();
        mainPanel.endProcess();
        String modeStr = getModeStr(mode);
        mainPanel.appendResult(modeStr + "完成");
    }

    private static void searchDirectory(int mode, List<File> fileList, File root) throws IOException {
        if (root.isFile()) {
            fileList.add(root);
        } else if (root.isDirectory()) {
            File[] subFiles = root.listFiles();
            for (int i = 0; i < subFiles.length; ++i) {
                File file = subFiles[i];
                // 解密模式下跳过保存文件夹名称的文件
                int idxOfDot = file.getName().lastIndexOf(".");
                if (idxOfDot != -1) {
                    String fileSuffix = file.getName().substring(idxOfDot + 1);
                    if (mode == DECRYPT && nameStorageSuffix.equals(fileSuffix)) {
                        continue;
                    }
                }
                searchDirectory(mode, fileList, file);
            }
        }
    }

    private static void cryptDirectory(int mode, String password, List<File> fileList, File root, MainPanel mainPanel)
            throws IOException {
        if (root.isFile()) {
            fileList.add(root);
        } else if (root.isDirectory()) {
            String algorString = "AES/ECB/PKCS5Padding";
            byte[] pwdMd5Bytes = CryptUtil.md5(password).getBytes();
            if (mode == ENCRYPT) {
                // 加密模式，给文件夹重命名为UUID，并在文件夹内新建一个文件用于保存密码的MD5以及加密过的原来的文件名
                // 密码的MD5是用来校验输入密码是否正确的，长度为固定的32位
                byte[] oriNameBytes = CryptUtil.aesEncrypt(password, root.getName().getBytes(), algorString);
                byte[] contentBytes = new byte[pwdMd5Bytes.length + oriNameBytes.length];
                // 将密码的MD5和原来的文件名内容合并成一个数组
                System.arraycopy(pwdMd5Bytes, 0, contentBytes, 0, pwdMd5Bytes.length);
                System.arraycopy(oriNameBytes, 0, contentBytes, pwdMd5Bytes.length, oriNameBytes.length);
                String uuid = CryptUtil.getUUID();
                String newName = root.getParent() + File.separator + uuid + "." + directorySuffix;
                String nameStorageFileName = root.getAbsolutePath() + File.separator + uuid + "." + nameStorageSuffix;
                Files.write(Paths.get(nameStorageFileName), contentBytes);
                if (root.renameTo(new File(newName))) {
                    root = new File(newName);
                }
            } else if (mode == DECRYPT) {
                // 解密模式，从保存文件名的文件中恢复出文件夹的名字
                int idxOfDot = root.getName().lastIndexOf(".");
                String uuid = root.getName().substring(0, idxOfDot == -1 ? root.getName().length() : idxOfDot);
                String nameStorageFileName = root.getAbsolutePath() + File.separator + uuid + "." + nameStorageSuffix;
                File nameStorageFile = new File(nameStorageFileName);
                if (nameStorageFile.exists()) {
                    byte[] contentBytes = Files.readAllBytes(Paths.get(nameStorageFileName));
                    byte[] testPwdMd5Bytes = new byte[32];
                    byte[] oriNameBytes = new byte[contentBytes.length - 32];
                    System.arraycopy(contentBytes, 0, testPwdMd5Bytes, 0, 32);
                    // 校验密码是否正确
                    for (int i = 0; i < pwdMd5Bytes.length; ++i) {
                        if (pwdMd5Bytes[i] != testPwdMd5Bytes[i]) {
                            String msg = "对于文件夹" + root.getAbsolutePath() + "的密码错误";
                            mainPanel.appendResult(msg);
                            return;
                        }
                    }
                    System.arraycopy(contentBytes, 32, oriNameBytes, 0, oriNameBytes.length);
                    String oriNameStr = new String(CryptUtil.aesDecrypt(password, oriNameBytes, algorString));
                    String oriName = root.getParent() + File.separator + oriNameStr;
                    if (root.renameTo(new File(oriName))) {
                        root = new File(oriName);
                        nameStorageFileName = root.getAbsolutePath() + File.separator + uuid + "." + nameStorageSuffix;
                        Files.delete(Paths.get(nameStorageFileName));
                    }
                }
            }
            File[] subFiles = root.listFiles();
            for (int i = 0; i < subFiles.length; ++i) {
                File file = subFiles[i];
                // 加密模式下跳过保存文件夹名称的文件
                int idxOfDot = file.getName().lastIndexOf(".");
                if (idxOfDot != -1) {
                    String fileSuffix = file.getName().substring(idxOfDot + 1);
                    if (mode == ENCRYPT && nameStorageSuffix.equals(fileSuffix)) {
                        continue;
                    }
                }
                cryptDirectory(mode, password, fileList, file, mainPanel);
            }
        }
    }

    private static void encryptSingleFile(File file, String password, MainPanel mainPanel) throws IOException {
        // 获取原始文件名
        String oriName = file.getName();
        byte[] oriNameBytes = oriName.getBytes();
        // 修改文件名，改成UUID加默认后缀，注意，renameTo操作必须在raFile创建之前或关闭之后执行
        String newName = file.getParent() + File.separator + CryptUtil.getUUID() + "." + fileSuffix;
        if (file.renameTo(new File(newName))) {
            file = new File(newName);
        } else {
            mainPanel.appendResult(file.getAbsolutePath() + " 重命名失败");
            return;
        }
        // 文件名修改完成后再打开文件
        RandomAccessFile raFile = new RandomAccessFile(file, "rw");
        // 先填充文件，使其大小等于操作长度的倍数
        long oriFileLength = raFile.length();
        int paddingNum = paddingFile(raFile, oriFileLength);
        long fileLength = oriFileLength + paddingNum;
        // 写入当前加密到的位置，每次加密完一个块就更新进度，防止中途退出后丢失加密位置
        raFile.write(CryptUtil.getBytes(0L));
        long encryptProgressTailPos = fileLength;
        // 再写入原始文件名
        raFile.write(oriNameBytes);
        // 再写入原始文件名的长度，占4个字节
        raFile.write(CryptUtil.getBytes(oriNameBytes.length));
        // 再写入原始文件长度，占8个字节
        raFile.write(CryptUtil.getBytes(oriFileLength));
        // 再将加密密码的md5写入，作为校验字符串，占32个字节
        raFile.write(CryptUtil.md5(password).getBytes());
        // 加密文件的标志做好之后就开始正式加密操作了
        long curPos = 0;
        raFile.seek(0);
        byte[] buffer = new byte[blockSize];
        while (curPos != fileLength && raFile.read(buffer) != -1) {
            buffer = CryptUtil.aesEncrypt(password, buffer);
            raFile.seek(curPos);
            raFile.write(buffer);
            long prePos = curPos;
            curPos = getNextPosition(curPos, fileLength);
            updateProgress(curPos - prePos, mainPanel);
            raFile.seek(encryptProgressTailPos);
            // 每次写入加密数据后在文件尾记录当前的位置
            raFile.write(CryptUtil.getBytes(curPos));
            raFile.seek(curPos);
        }
        raFile.close();
        mainPanel.appendResult(oriName + " 加密成功");
    }

    private static void decryptSingleFile(File file, String password, MainPanel mainPanel) throws IOException {
        RandomAccessFile raFile = new RandomAccessFile(file, "rw");
        // 文件长度小于40字节，则一定不是加密后的文件，因为校验字符串加上文件长度就已经有40字节
        if (raFile.length() < 40) {
            mainPanel.appendResult(file.getAbsolutePath() + " 不是加密文件");
            raFile.close();
            return;
        }
        // 校验文件是否以校验字符串结尾
        // 先跳转到文件末尾
        long curPos = raFile.length() - 32;
        raFile.seek(curPos);
        byte[] validBytes = new byte[32];
        byte[] pwdBytes = CryptUtil.md5(password).getBytes();
        raFile.read(validBytes);
        // 逐个字节校验是否相等
        for (int i = 0; i < 32; ++i) {
            if (validBytes[i] != pwdBytes[i]) {
                mainPanel.appendResult(file.getAbsolutePath() + " 不是加密文件或密码不正确");
                raFile.close();
                return;
            }
        }
        // 获取文件原始长度
        byte[] oriLengthBytes = new byte[8];
        curPos -= 8;
        raFile.seek(curPos);
        raFile.read(oriLengthBytes);
        long oriFileLength = CryptUtil.getLong(oriLengthBytes);
        int paddingNum = getFileLengthRemain(oriFileLength);
        long fileLength = oriFileLength + paddingNum;
        // 获取原始文件名长度
        byte[] oriFileNameLengthBytes = new byte[4];
        curPos -= 4;
        raFile.seek(curPos);
        raFile.read(oriFileNameLengthBytes);
        int oriFileNameLength = CryptUtil.getInteger(oriFileNameLengthBytes);
        // 获取原始文件名
        byte[] oriFileNameBytes = new byte[oriFileNameLength];
        curPos -= oriFileNameLength;
        raFile.seek(curPos);
        raFile.read(oriFileNameBytes);
        String oriFileName = new String(oriFileNameBytes);
        // 获取加密进度，如果加密过程中途退出，则加密进度不等于文件填充后的长度
        byte[] encryptProgressPosBytes = new byte[8];
        curPos -= 8;
        raFile.seek(curPos);
        raFile.read(encryptProgressPosBytes);
        long encryptProgressPos = CryptUtil.getLong(encryptProgressPosBytes);
        // 解析完文件尾信息，开始正式解密数据
        byte[] buffer = new byte[blockSize];
        curPos = 0;
        raFile.seek(0);
        // 解密时，由于文件中包含加密时的附加内容，所以不能一直解密到文件尾
        while (curPos != encryptProgressPos && raFile.read(buffer) != -1) {
            buffer = CryptUtil.aesDecrypt(password, buffer);
            raFile.seek(curPos);
            raFile.write(buffer);
            long prePos = curPos;
            curPos = getNextPosition(curPos, fileLength);
            updateProgress(curPos - prePos, mainPanel);
            raFile.seek(curPos);
        }
        // 通过将文件长度设置为原始长度来清除多余内容
        raFile.setLength(oriFileLength);
        raFile.close();
        // 关闭文件后修改文件名
        String oriName = file.getParent() + File.separator + oriFileName;
        String encryptedFileName = file.getAbsolutePath();
        if (file.renameTo(new File(oriName))) {
            mainPanel.appendResult(oriFileName + " 解密成功");
        } else {
            mainPanel.appendResult(encryptedFileName + " 重命名失败");
        }
    }

    private static void updateProgress(long size, MainPanel mainPanel) {
        long tmp = curTotalFinishedFileSize.addAndGet(size);
        double progress = (double) tmp * 100.0 / (double) curTotalListedFileSize;
        String progressStr = String.format("%.2f", progress) + "%";
        mainPanel.updateProgress(progressStr);
    }

    /**
     * 计算下一步个要加密的位置
     * 
     * @param curPosition 当前位置
     * @param fileLength  文件总长度（填充后的）
     * @return
     */
    private static long getNextPosition(long curPosition, long fileLength) {
        if (curPosition < 1048576 || fileLength - curPosition < 524288) {
            // 文件前1M和后512k的内容全部加密
            return curPosition + blockSize;
        } else if (fileLength > 16777216L) {
            // 若文件大于16M，每8块加密1块
        } else if (fileLength > 67108864L) {
            // 若文件大于64M，每16块加密1块
            return curPosition + blockSize * 8;
        } else if (fileLength > 134217728L) {
            // 若文件大于256M，每32块加密1块
            return curPosition + blockSize * 16;
        } else if (fileLength > 1073741824L) {
            // 若文件大于1G，每64块加密1块
            return curPosition + blockSize * 32;
        }
        return curPosition + blockSize;
    }

    /**
     * 如果文件大小不是blockSize的倍数，则填充0使其成为blcokSize的倍数，便于操作。 <b>填充操作会使将文件指针移动到文件末尾</b>
     * 
     * @param raFile 文件对象
     * @return 填充的字节数
     * @throws IOException
     */
    private static int paddingFile(RandomAccessFile raFile, long oriFileLength) throws IOException {
        int fileLengthRemain = getFileLengthRemain(oriFileLength);
        raFile.seek(raFile.length());
        if (fileLengthRemain != 0) {
            byte[] fillBytes = new byte[fileLengthRemain];
            for (int i = 0; i < fileLengthRemain; ++i) {
                fillBytes[i] = 0;
            }
            raFile.write(fillBytes);
        }
        return fileLengthRemain;
    }

    private static int getFileLengthRemain(long oriFileLength) {
        return blockSize - (int) (oriFileLength % blockSize);
    }
}
