package video;
import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
@IFMLLoadingPlugin.Name("CleanroomVideoPrototype")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"video."})
public final class Plugin implements IFMLLoadingPlugin {
 public String[] getASMTransformerClass(){return new String[]{"video.Transformer"};}
 public void injectData(Map<String,Object> data){Object d=data.get("mcLocation"); if(d!=null) System.setProperty("cleanroom.video.gameDir",d.toString());}
}
