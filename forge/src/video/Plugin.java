package video;
import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
@IFMLLoadingPlugin.Name("LoadingVideoForge")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"video."})
public final class Plugin implements IFMLLoadingPlugin {
 public String[] getASMTransformerClass(){return new String[]{"video.Transformer"};}
 public String getModContainerClass(){return null;} public String getSetupClass(){return null;} public String getAccessTransformerClass(){return null;}
 public void injectData(Map<String,Object> data){Object d=data.get("mcLocation"); if(d!=null) System.setProperty("cleanroom.video.gameDir",d.toString());}
}
