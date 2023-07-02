package cc.paimonmc.jbyte;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * by paimonmc
 */
public class AutoJbyte extends Application {
    private ClassNode classNode;
    private File jar;
    private String newName;
    private final String CLASS_NAME = "net/ccbluex/liquidbounce/LiquidBounce.class";
    public static void main(String[] args) {
        AutoJbyte.launch(args);
    }
    private void load(File jar) throws IOException {
        ZipFile zipFile = new ZipFile(jar);
        zipFile.stream().forEach(zipEntry -> {
            if (zipEntry.getName().equals(CLASS_NAME)){
                try {
                    ClassReader reader = new ClassReader(zipFile.getInputStream(zipEntry));
                    classNode = new ClassNode();
                    reader.accept(classNode,ClassReader.EXPAND_FRAMES);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    private void save(File saveJar) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(saveJar.toPath()))) {
            ZipFile zipFile = new ZipFile(jar);
            zipFile.stream().forEach(zipEntry -> {
                try {
                    if (!zipEntry.getName().equals(CLASS_NAME)){
                        zipOutputStream.putNextEntry(zipEntry);
                        InputStream inputStream = zipFile.getInputStream(zipEntry);
                        byte[] bytes = new byte[1024];
                        int length;
                        while ((length = inputStream.read(bytes)) != -1){
                            zipOutputStream.write(bytes,0,length);
                        }
                        inputStream.close();
                        zipOutputStream.closeEntry();
                    } else {
                        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        classNode.accept(classWriter);
                        zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
                        zipOutputStream.write(classWriter.toByteArray());
                        zipOutputStream.closeEntry();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        AnchorPane root = new AnchorPane();
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        jar = FxUtil.openFileChooser("选择Jar","jar");
        load(jar);
        newName = JOptionPane.showInputDialog("你想修改的名字");
        classNode.methods.forEach(methodNode -> {
            if (methodNode.name.equals("<clinit>")){
                methodNode.instructions.clear();
                InsnList insnList = new InsnList();
                insnList.add(new TypeInsnNode(Opcodes.NEW,"net/ccbluex/liquidbounce/LiquidBounce"));
                insnList.add(new InsnNode(Opcodes.DUP));
                insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,"net/ccbluex/liquidbounce/LiquidBounce","<init>","()V"));
                insnList.add(new VarInsnNode(Opcodes.ASTORE,0));
                insnList.add(new VarInsnNode(Opcodes.ALOAD,0));
                insnList.add(new FieldInsnNode(Opcodes.PUTSTATIC,"net/ccbluex/liquidbounce/LiquidBounce","INSTANCE","Lnet/ccbluex/liquidbounce/LiquidBounce;"));
                insnList.add(new LdcInsnNode(newName));
                insnList.add(new FieldInsnNode(Opcodes.PUTSTATIC,"net/ccbluex/liquidbounce/LiquidBounce","CLIENT_NAME","Ljava/lang/String;"));
                insnList.add(new InsnNode(Opcodes.RETURN));
                methodNode.instructions = insnList;
            }
            if (methodNode.name.equals("startClient") && methodNode.desc.equals("()V")){
                for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
                    if (insnNode instanceof JumpInsnNode){
                        if (insnNode.getOpcode() == Opcodes.IFNE && insnNode.getPrevious().getOpcode() == Opcodes.INVOKESTATIC && insnNode.getPrevious().getPrevious().getOpcode() == Opcodes.ACONST_NULL){
                            methodNode.instructions.set(insnNode,new JumpInsnNode(Opcodes.IFEQ,((JumpInsnNode) insnNode).label));
                        }
                    }
                }
            }
        });
        save(new File("output.jar"));
        JOptionPane.showMessageDialog(null,"修改成功,点击确定以退出");
        System.exit(0);
    }
}
