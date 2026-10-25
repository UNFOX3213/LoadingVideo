package video;
import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public final class Verify {
 static byte[] bytes(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
 public static void main(String[] args)throws Exception{
  for(String path:args){
   byte[] input;try(JarFile jar=new JarFile(path)){input=bytes(jar.getInputStream(jar.getJarEntry("net/minecraftforge/fml/client/SplashProgress$2.class")));}
   Transformer t=new Transformer();byte[] output=t.transform("net.minecraftforge.fml.client.SplashProgress$2","net.minecraftforge.fml.client.SplashProgress$2",input);
   ClassNode c=new ClassNode();new ClassReader(output).accept(c,0);int draw=0,stop=0,close=0;
   for(Object obj:c.methods){MethodNode m=(MethodNode)obj;for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode){MethodInsnNode x=(MethodInsnNode)n;if(!x.owner.equals("video/Player"))continue;if(x.name.equals("draw") && x.desc.equals("(II)V"))draw++;if(x.name.equals("shouldStop"))stop++;if(x.name.equals("close"))close++;}}
   if(draw!=1 || stop!=1 || close!=1)throw new AssertionError("Wrong hooks: "+draw+","+stop+","+close);
   if(t.transform("unrelated","unrelated",input)!=input)throw new AssertionError("Unrelated class modified");
   if(output[6]!=input[6] || output[7]!=input[7])throw new AssertionError("Changed target bytecode version");
   System.out.println("PASS splash hooks: "+new File(path).getName());
  }
  System.setProperty("cleanroom.video.gameDir",Files.createTempDirectory("loading-video-missing-").toString());Player.init();
  if(!Player.ended || !Player.shouldStop(true) || Player.shouldStop(false))throw new AssertionError("Missing-video fallback");
  System.out.println("PASS missing-video fallback and loading completion");
 }
}
