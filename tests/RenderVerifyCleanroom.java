package video;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import java.nio.*;
public class RenderVerifyCleanroom {
 public static void main(String[] args){
  if(!GLFW.glfwInit())throw new AssertionError("GLFW init failed");
  GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
  long w=GLFW.glfwCreateWindow(64,64,"Video verification",0,0);if(w==0)throw new AssertionError("Core profile unavailable");
  try{GLFW.glfwMakeContextCurrent(w);GL.createCapabilities();
   byte[] frame=new byte[Player.SIZE];for(int i=0;i<frame.length;i+=3)frame[i]=(byte)255;
   GL11.glViewport(3,4,20,22);GL11.glEnable(GL11.GL_DEPTH_TEST);
   Player.initialized=true;Player.latest=frame;Player.draw(64,64);
   if(Player.closed || !Player.firstDraw)throw new AssertionError("Full playback entry failed");
   ByteBuffer pixel=ByteBuffer.allocateDirect(4);GL11.glReadPixels(32,32,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
   if((pixel.get(0)&255)<250 || (pixel.get(1)&255)>5 || (pixel.get(2)&255)>5)throw new AssertionError("Incorrect rendered pixel");
   IntBuffer vp=ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asIntBuffer();GL11.glGetIntegerv(GL11.GL_VIEWPORT,vp);
   if(vp.get(0)!=3 || vp.get(1)!=4 || vp.get(2)!=20 || vp.get(3)!=22 || !GL11.glIsEnabled(GL11.GL_DEPTH_TEST))throw new AssertionError("GL state not restored");
   // Simulate the loader drawing its next screen before the overlay hook.
   GL11.glClearColor(0,0,1,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);Player.ended=true;Player.draw(64,64);
   pixel.clear();GL11.glReadPixels(32,32,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
   if((pixel.get(0)&255)>5 || (pixel.get(2)&255)<250 || !Player.returnedToSplash || Player.latest!=null)throw new AssertionError("Finished video overwrote loading screen");
   if(Player.shouldStop(false) || !Player.shouldStop(true))throw new AssertionError("Loading completion behavior changed");
   int error=GL11.glGetError();if(error!=0)throw new AssertionError("GL error "+error);Player.close();System.out.println("PASS: OpenGL core-profile rendering, state restoration, and return to loading screen; "+GL11.glGetString(GL11.GL_VERSION));
  }finally{GLFW.glfwDestroyWindow(w);GLFW.glfwTerminate();}
 }
}
