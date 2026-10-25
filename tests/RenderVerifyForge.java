package video;
import org.lwjgl.opengl.*;
import java.nio.*;
public final class RenderVerifyForge {
 public static void main(String[] args)throws Exception{
  Display.setDisplayMode(new DisplayMode(64,64));Display.setTitle("LoadingVideo Forge verification");Display.create(new PixelFormat(),new ContextAttribs(3,2).withProfileCore(true));
  try{
   byte[] frame=new byte[Player.SIZE];for(int i=0;i<frame.length;i+=3)frame[i]=(byte)255;
   GL11.glViewport(3,4,20,22);GL11.glEnable(GL11.GL_DEPTH_TEST);Player.initialized=true;Player.latest=frame;Player.draw(64,64);
   if(Player.closed || !Player.firstDraw)throw new AssertionError("Full playback entry failed");
   ByteBuffer pixel=ByteBuffer.allocateDirect(4);GL11.glReadPixels(32,32,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
   if((pixel.get(0)&255)<250 || (pixel.get(1)&255)>5 || (pixel.get(2)&255)>5)throw new AssertionError("Incorrect video pixel");
   IntBuffer vp=ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer();GL11.glGetInteger(GL11.GL_VIEWPORT,vp);
   if(vp.get(0)!=3 || vp.get(1)!=4 || vp.get(2)!=20 || vp.get(3)!=22 || !GL11.glIsEnabled(GL11.GL_DEPTH_TEST))throw new AssertionError("State not restored");
   GL11.glClearColor(0,0,1,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);Player.ended=true;Player.draw(64,64);pixel.clear();GL11.glReadPixels(32,32,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
   if((pixel.get(0)&255)>5 || (pixel.get(2)&255)<250 || !Player.returnedToSplash || Player.latest!=null)throw new AssertionError("Ended overlay covered loader");
   if(Player.shouldStop(false) || !Player.shouldStop(true))throw new AssertionError("Loading wait behavior");
   int err=GL11.glGetError();if(err!=0)throw new AssertionError("GL error "+err);Player.close();
   System.out.println("PASS Forge LWJGL2 core-profile rendering, state restoration, and return to splash; Java "+System.getProperty("java.version"));
  }finally{Display.destroy();}
 }
}
