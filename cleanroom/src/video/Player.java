package video;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.*;
import org.lwjgl.opengl.GL11;
/** Experimental fixed 640x360/30fps decoder, bounded to two RGB frames. */
public final class Player {
 static final int W=640,H=360,SIZE=W*H*3;
 static volatile boolean initialized,ended,closed;
 static volatile byte[] latest;
 static volatile SourceDataLine line;
 static volatile Process decoder;
 static long started;
 static int texture, program, vao;
 static boolean firstDraw, returnedToSplash;
 static ByteBuffer pixels;
 static final java.util.List<Process> processes=new java.util.concurrent.CopyOnWriteArrayList<>();
 static synchronized void init(){
  if(initialized)return; initialized=true;
  try{
   Path root=Paths.get(System.getProperty("cleanroom.video.gameDir","."),"config","cleanroom-video");
   Properties p=new Properties();Path cfg=root.resolve("video.properties");
   if(Files.exists(cfg))try(InputStream in=Files.newInputStream(cfg)){p.load(in);}
   Path movie=root.resolve(p.getProperty("video","video.mp4")).normalize();
   System.out.println("[CleanroomVideo] Playback init: "+movie.toAbsolutePath());
   if(!Files.isRegularFile(movie)){System.err.println("[CleanroomVideo] Video is missing; using original splash");ended=true;return;}
   String ff=p.getProperty("ffmpeg","ffmpeg");
   Process video=new ProcessBuilder(ff,"-nostdin","-v","error","-re","-i",movie.toString(),"-an","-vf","scale=640:360:force_original_aspect_ratio=decrease,pad=640:360:(ow-iw)/2:(oh-ih)/2,fps=30","-pix_fmt","rgb24","-f","rawvideo","pipe:1").redirectError(root.resolve("video-error.log").toFile()).start();
   decoder=video;processes.add(video);started=System.nanoTime();
   Thread frames=new Thread(()->{
    try(InputStream in=video.getInputStream()){
     while(!closed){byte[] frame=in.readNBytes(SIZE);if(frame.length!=SIZE)break;if(latest==null)System.out.println("[CleanroomVideo] First decoded RGB frame ready");latest=frame;}
    }catch(Exception e){if(!closed)e.printStackTrace();}finally{ended=true;video.destroy();}
   },"Cleanroom video decoder");frames.setDaemon(true);frames.start();
   Thread audio=new Thread(()->playAudio(ff,movie,root),"Cleanroom video audio");audio.setDaemon(true);audio.start();
   Runtime.getRuntime().addShutdownHook(new Thread(Player::shutdown,"Cleanroom video shutdown"));
  }catch(Exception e){ended=true;shutdown();System.err.println("[CleanroomVideo] Falling back to splash: "+e);}
 }
 static void playAudio(String ff,Path movie,Path root){
  try{
   Process a=new ProcessBuilder(ff,"-nostdin","-v","error","-i",movie.toString(),"-vn","-ac","2","-ar","48000","-f","s16le","pipe:1").redirectError(root.resolve("audio-error.log").toFile()).start();processes.add(a);
   if(closed){a.destroy();return;}
   SourceDataLine out=AudioSystem.getSourceDataLine(new AudioFormat(48000,16,2,true,false));line=out;
   out.open(new AudioFormat(48000,16,2,true,false),19200);out.start();
   try(InputStream in=a.getInputStream()){byte[] b=new byte[8192];int n;while(!closed && (n=in.read(b))!=-1){int off=0;while(!closed && off<n)off+=out.write(b,off,n-off);}}
   if(!closed)out.drain();out.close();a.destroy();
  }catch(Exception e){System.err.println("[CleanroomVideo] Audio unavailable: "+e);}
 }
 public static boolean shouldStop(boolean done){
  // The existing splash loop retains ownership of its GL context until video EOF.
  return done && (!initialized || ended || closed || System.nanoTime()-started>600_000_000_000L);
 }
 public static void draw(int width,int height){
  try{
   init();if(closed)return;
   if(ended){
    if(!returnedToSplash){returnedToSplash=true;latest=null;System.out.println("[CleanroomVideo] Video ended; restored original loading screen");}
    return;
   }
   byte[] frame=latest;if(frame==null)return;
   // The loader owns the native window; do not initialize or query a second Display.
   try{org.lwjgl.opengl.GL.getCapabilities();}catch(IllegalStateException e){org.lwjgl.opengl.GL.createCapabilities();}
   render(frame,width,height);
   if(!firstDraw){firstDraw=true;System.out.println("[CleanroomVideo] First frame drawn: "+width+"x"+height+", GL "+GL11.glGetString(GL11.GL_VERSION));}
  }catch(Throwable e){System.err.println("[CleanroomVideo] Playback failed: "+e);e.printStackTrace();ended=true;shutdown();}
 }
 static int shader(int type,String source){
  int s=org.lwjgl.opengl.GL20.glCreateShader(type);org.lwjgl.opengl.GL20.glShaderSource(s,source);org.lwjgl.opengl.GL20.glCompileShader(s);
  if(org.lwjgl.opengl.GL20.glGetShaderi(s,org.lwjgl.opengl.GL20.GL_COMPILE_STATUS)==0){String log=org.lwjgl.opengl.GL20.glGetShaderInfoLog(s);org.lwjgl.opengl.GL20.glDeleteShader(s);throw new IllegalStateException(log);}return s;
 }
 static void render(byte[] frame,int width,int height){
  int oldProgram=GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM), oldVao=GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING);
  int active=GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE),unpack=GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
  java.nio.IntBuffer view=ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asIntBuffer();GL11.glGetIntegerv(GL11.GL_VIEWPORT,view);
  boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
  org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);int binding=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
  try{
   if(program==0){
    int v=shader(org.lwjgl.opengl.GL20.GL_VERTEX_SHADER,"#version 150\nout vec2 uv;void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);uv=vec2(p.x,1.0-p.y);gl_Position=vec4(p*2.0-1.0,0,1);}");
    int f=shader(org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER,"#version 150\nuniform sampler2D movie;in vec2 uv;out vec4 color;void main(){color=texture(movie,uv);}");
    program=org.lwjgl.opengl.GL20.glCreateProgram();org.lwjgl.opengl.GL20.glAttachShader(program,v);org.lwjgl.opengl.GL20.glAttachShader(program,f);org.lwjgl.opengl.GL20.glLinkProgram(program);org.lwjgl.opengl.GL20.glDeleteShader(v);org.lwjgl.opengl.GL20.glDeleteShader(f);
    if(org.lwjgl.opengl.GL20.glGetProgrami(program,org.lwjgl.opengl.GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(org.lwjgl.opengl.GL20.glGetProgramInfoLog(program));
    vao=org.lwjgl.opengl.GL30.glGenVertexArrays();
   }
   if(texture==0){texture=GL11.glGenTextures();pixels=ByteBuffer.allocateDirect(SIZE);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGB,W,H,0,GL11.GL_RGB,GL11.GL_UNSIGNED_BYTE,(ByteBuffer)null);}
   GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
   GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,1);pixels.clear();pixels.put(frame).flip();GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,W,H,GL11.GL_RGB,GL11.GL_UNSIGNED_BYTE,pixels);
   org.lwjgl.opengl.GL20.glUseProgram(program);org.lwjgl.opengl.GL20.glUniform1i(org.lwjgl.opengl.GL20.glGetUniformLocation(program,"movie"),0);org.lwjgl.opengl.GL30.glBindVertexArray(vao);
   GL11.glViewport(0,0,width,height);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);
  }finally{
   org.lwjgl.opengl.GL20.glUseProgram(oldProgram);org.lwjgl.opengl.GL30.glBindVertexArray(oldVao);GL11.glBindTexture(GL11.GL_TEXTURE_2D,binding);org.lwjgl.opengl.GL13.glActiveTexture(active);GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,unpack);GL11.glViewport(view.get(0),view.get(1),view.get(2),view.get(3));
   enabled(GL11.GL_DEPTH_TEST,depth);enabled(GL11.GL_BLEND,blend);enabled(GL11.GL_SCISSOR_TEST,scissor);enabled(GL11.GL_CULL_FACE,cull);
  }
 }
 static void enabled(int cap,boolean value){if(value)GL11.glEnable(cap);else GL11.glDisable(cap);}
 static void shutdown(){closed=true;for(Process p:processes)p.destroyForcibly();SourceDataLine l=line;if(l!=null){l.stop();l.flush();l.close();}}
 public static void close(){shutdown();if(texture!=0){GL11.glDeleteTextures(texture);texture=0;}if(program!=0){org.lwjgl.opengl.GL20.glDeleteProgram(program);program=0;}if(vao!=0){org.lwjgl.opengl.GL30.glDeleteVertexArrays(vao);vao=0;}pixels=null;latest=null;}
}
