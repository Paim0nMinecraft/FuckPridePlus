package cc.paimonmc.fuckprideplus;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Auto Edit Client Name
 */
public class AutoEdit extends Application {
    private ClassNode module;
    private final List<ClassNode> classNodes = new ArrayList<>();
    private File jar;
    private String newName;
    public static void main(String[] args) {
        AutoEdit.launch(args);
    }
    private void load(File jar) throws IOException {
        ZipFile zipFile = new ZipFile(jar);
        zipFile.stream().forEach(zipEntry -> {
            if (zipEntry.getName().endsWith(".class")){
                try {
                    ClassNode classNode = new ClassNode();
                    ClassReader reader = new ClassReader(zipFile.getInputStream(new ZipEntry(zipEntry)));
                    reader.accept(classNode,ClassReader.EXPAND_FRAMES);
                    classNodes.add(classNode);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    private void save(File saveJar) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(saveJar.toPath()))) {
            for (ClassNode classNode : classNodes) {
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(classWriter);
                zipOutputStream.putNextEntry(new ZipEntry(classNode.name + ".class"));
                zipOutputStream.write(classWriter.toByteArray());
                zipOutputStream.closeEntry();
            }
            ZipFile zipFile = new ZipFile(jar);
            zipFile.stream()
                    .filter(zipEntry -> !getClassName().contains(zipEntry.getName()))
                    .forEach(zipEntry -> {
                try {
                    zipOutputStream.putNextEntry(zipEntry);
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = inputStream.read(bytes)) != -1){
                        zipOutputStream.write(bytes,0,length);
                    }
                    inputStream.close();
                    zipOutputStream.closeEntry();
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
        int result = JOptionPane.showConfirmDialog(null,"是否想添加新的模块到此外挂");
        if (result == JOptionPane.YES_OPTION){
            module = new ClassNode();
            ClassReader reader = new ClassReader(Files.newInputStream(FxUtil.openFileChooser("选择你想要添加的模块的class文件", "class").toPath()));
            reader.accept(module,ClassReader.EXPAND_FRAMES);
            if (getClassName().contains(module.name + ".class")) {
                JOptionPane.showMessageDialog(null,"此模块已经存在!");
            } else {
                classNodes.add(module);
                JOptionPane.showMessageDialog(null,"添加成功");
            }
        }
        classNodes.forEach(node -> {
            switch (node.name){
                case "net/ccbluex/liquidbounce/LiquidBounce":{
                    node.methods.forEach(methodNode -> {
                        if (methodNode.name.equals("<clinit>")) {
                            methodNode.instructions.clear();
                            InsnList insnList = new InsnList();
                            insnList.add(new TypeInsnNode(Opcodes.NEW, "net/ccbluex/liquidbounce/LiquidBounce"));
                            insnList.add(new InsnNode(Opcodes.DUP));
                            insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/ccbluex/liquidbounce/LiquidBounce", "<init>", "()V"));
                            insnList.add(new VarInsnNode(Opcodes.ASTORE, 0));
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            insnList.add(new FieldInsnNode(Opcodes.PUTSTATIC, "net/ccbluex/liquidbounce/LiquidBounce", "INSTANCE", "Lnet/ccbluex/liquidbounce/LiquidBounce;"));
                            insnList.add(new LdcInsnNode(newName));
                            insnList.add(new FieldInsnNode(Opcodes.PUTSTATIC, "net/ccbluex/liquidbounce/LiquidBounce", "CLIENT_NAME", "Ljava/lang/String;"));
                            insnList.add(new InsnNode(Opcodes.RETURN));
                            methodNode.instructions = insnList;
                        }
                        if (methodNode.name.equals("startClient") && methodNode.desc.equals("()V")) {
                            for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
                                if (insnNode instanceof JumpInsnNode) {
                                    if (insnNode.getOpcode() == Opcodes.IFNE && insnNode.getPrevious().getOpcode() == Opcodes.INVOKESTATIC && insnNode.getPrevious().getPrevious().getOpcode() == Opcodes.ACONST_NULL) {
                                        methodNode.instructions.set(insnNode, new JumpInsnNode(Opcodes.IFEQ, ((JumpInsnNode) insnNode).label));
                                    }
                                }
                            }
                        }
                    });
                    break;
                }
                case "net/ccbluex/liquidbounce/features/module/ModuleManager":{
                    node.methods.forEach(methodNode -> {
                        if (methodNode.name.equals("registerModules") && methodNode.desc.equals("()V")){
                            if (classNodes.contains(module)){
                                Arrays.stream(methodNode.instructions.toArray())
                                        .filter(insnNode -> insnNode.getOpcode() == Opcodes.SIPUSH)
                                        .findFirst()
                                        .ifPresent(insnNode -> {
                                            ((IntInsnNode)insnNode).operand++;
                                            Arrays.stream(methodNode.instructions.toArray())
                                                    .filter(insnNode2 -> insnNode2.getOpcode() == Opcodes.INVOKEVIRTUAL)
                                                    .findFirst()
                                                    .ifPresent(insnNode2 -> {
                                                        InsnList insnList = new InsnList();
                                                        insnList.add(new InsnNode(Opcodes.DUP));
                                                        insnList.add(new IntInsnNode(Opcodes.SIPUSH,((IntInsnNode)insnNode2.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious()).operand + 1));
                                                        insnList.add(new LdcInsnNode(Type.getType(getDescriptor(module.name))));
                                                        insnList.add(new InsnNode(Opcodes.AASTORE));
                                                        methodNode.instructions.insertBefore(insnNode2,insnList);
                                                    });
                                        });
                            }
                        }
                    });
                }
            }
        });
        save(new File("output.jar"));
        JOptionPane.showMessageDialog(null,"修改成功,点击确定以退出");
        System.exit(0);
    }
    private List<String> getClassName(){
        List<String> list = new ArrayList<>();
        for (ClassNode node : classNodes) {
            list.add(node.name + ".class");
        }
        return list;
    }
    private String getDescriptor(String name){
        return "L" + name + ";";
    }
}
