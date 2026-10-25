package video;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public final class Transformer implements IClassTransformer {
 public byte[] transform(String name,String mapped,byte[] bytes){
  if(bytes==null || !"net.minecraftforge.fml.client.SplashProgress$2".equals(mapped))return bytes;
  ClassNode c=new ClassNode(); new ClassReader(bytes).accept(c,0);
  int draw=0,stop=0,close=0;
  for(Object method:c.methods){MethodNode m=(MethodNode)method;if(!m.name.equals("run"))continue;
   for(AbstractInsnNode n:m.instructions.toArray()){
    if(n instanceof MethodInsnNode){MethodInsnNode x=(MethodInsnNode)n;
     if(x.owner.equals("net/minecraftforge/fml/client/SplashProgress") && x.name.equals("access$400") && x.desc.equals("()Z")){m.instructions.insert(n,new MethodInsnNode(Opcodes.INVOKESTATIC,"video/Player","shouldStop","(Z)Z",false));stop++;}
     if(x.owner.endsWith("/Display") && x.name.equals("update") && x.desc.equals("()V")){
      InsnList hook=new InsnList();
      hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,x.owner,"getWidth","()I",false));
      hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,x.owner,"getHeight","()I",false));
      hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"video/Player","draw","(II)V",false));
      m.instructions.insertBefore(n,hook);draw++;
     }
     AbstractInsnNode next=n.getNext();while(next!=null && next.getOpcode()<0)next=next.getNext();
     if(x.name.equals("clearGL") && x.owner.equals(c.name) && next!=null && next.getOpcode()==Opcodes.RETURN){
      m.instructions.insertBefore(n,new MethodInsnNode(Opcodes.INVOKESTATIC,"video/Player","close","()V",false));close++;
     }
    }
    if(n instanceof FieldInsnNode){FieldInsnNode f=(FieldInsnNode)n;
     if(f.getOpcode()==Opcodes.GETSTATIC && f.name.equals("done") && f.desc.equals("Z")){
      m.instructions.insert(n,new MethodInsnNode(Opcodes.INVOKESTATIC,"video/Player","shouldStop","(Z)Z",false));stop++;
     }
    }
   }
  }
  if(draw!=1 || stop!=1 || close!=1)throw new IllegalStateException("Unsupported SplashProgress bytecode: "+draw+","+stop+","+close);
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);
  System.out.println("[CleanroomVideo] Installed splash hooks");return w.toByteArray();
 }
}
