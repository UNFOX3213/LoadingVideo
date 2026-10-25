param(
 [ValidateSet('forge','cleanroom','all')][string]$Target='forge',
 [string]$MinecraftLibraries,
 [string]$Java8,
 [switch]$RenderTest
)
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$cache=Join-Path $root '.deps'
$dist=Join-Path $root 'dist'
New-Item -ItemType Directory -Force $cache,$dist | Out-Null
function Dependency([string]$name,[string]$relative,[string]$url){
 if($MinecraftLibraries){$local=Join-Path $MinecraftLibraries $relative;if(Test-Path -LiteralPath $local){return $local}}
 $path=Join-Path $cache $name
 if(!(Test-Path -LiteralPath $path)){if(!$url){throw "Dependency missing: $relative. Supply -MinecraftLibraries."};Invoke-WebRequest -Uri $url -OutFile $path}
 return $path
}
function CheckExit([string]$label){if($LASTEXITCODE -ne 0){throw "$label failed ($LASTEXITCODE)"}}
$targets=if($Target -eq 'all'){@('forge','cleanroom')}else{@($Target)}
foreach($kind in $targets){
 $forge=Dependency 'forge-1.12.2-14.23.5.2864.jar' 'net\minecraftforge\forge\1.12.2-14.23.5.2864\forge-1.12.2-14.23.5.2864.jar' 'https://maven.minecraftforge.net/net/minecraftforge/forge/1.12.2-14.23.5.2864/forge-1.12.2-14.23.5.2864-universal.jar'
 $launch=Dependency 'launchwrapper-1.12.jar' 'net\minecraft\launchwrapper\1.12\launchwrapper-1.12.jar' 'https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar'
 $asm=Dependency 'asm-all-5.2.jar' 'org\ow2\asm\asm-all\5.2\asm-all-5.2.jar' 'https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/5.2/asm-all-5.2.jar'
 $sources=@(Get-ChildItem -LiteralPath (Join-Path $root "$kind/src/video") -Filter '*.java' | Select-Object -ExpandProperty FullName)
 $verify=Join-Path $root 'tests/Verify.java'
 if($kind -eq 'forge'){
  $gl=Dependency 'lwjgl-2.9.4.jar' 'org\lwjgl\lwjgl\lwjgl\2.9.4-nightly-20150209\lwjgl-2.9.4-nightly-20150209.jar' 'https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar'
  $cp=@($forge,$launch,$asm,$gl) -join ';';$release=8
  $sources+=Join-Path $root 'tests/RenderVerifyForge.java'
  $runtime=if($Java8){$Java8}else{'java'}
 }else{
  $forge=Dependency 'cleanroom-0.6.13-alpha.jar' 'cleanroom-pcl2\net\minecraftforge\forge\1.12.2-14.23.5.2847\forge-1.12.2-14.23.5.2847.jar' ''
  $asmCore=Dependency 'asm-9.10.1.jar' 'org\ow2\asm\asm\9.10.1\asm-9.10.1.jar' 'https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar'
  $asmTree=Dependency 'asm-tree-9.10.1.jar' 'org\ow2\asm\asm-tree\9.10.1\asm-tree-9.10.1.jar' 'https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar'
  $gl=Dependency 'lwjgl-opengl-3.4.1.jar' 'org\lwjgl\lwjgl-opengl\3.4.1\lwjgl-opengl-3.4.1.jar' ''
  $core=Dependency 'lwjgl-3.4.1-unsafe.jar' 'org\lwjgl\lwjgl\3.4.1\lwjgl-3.4.1-unsafe.jar' ''
  $cp=@($forge,$launch,$asmCore,$asmTree,$gl,$core) -join ';';$release=25;$runtime='java'
 }
 $classes=Join-Path $root "build/$kind/classes";New-Item -ItemType Directory -Force $classes | Out-Null
 & javac --release $release -encoding UTF-8 -proc:none -cp $cp -d $classes $sources $verify;CheckExit 'Compile'
 $testJars=@($forge)
 if($kind -eq 'forge' -and $MinecraftLibraries){$old=Join-Path $MinecraftLibraries 'net\minecraftforge\forge\1.12.2-14.23.5.2847\forge-1.12.2-14.23.5.2847.jar';if(Test-Path -LiteralPath $old){$testJars+=$old}}
 & $runtime -cp "$classes;$cp" video.Verify $testJars;CheckExit 'Bytecode and fallback verification'
 if($RenderTest -and $kind -eq 'forge'){
  $nativeJar=Dependency 'lwjgl-2.9.4-natives-windows.jar' 'org\lwjgl\lwjgl\lwjgl-platform\2.9.4-nightly-20150209\lwjgl-platform-2.9.4-nightly-20150209-natives-windows.jar' 'https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-windows.jar'
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $natives=Join-Path $root 'build/forge/natives';New-Item -ItemType Directory -Force $natives | Out-Null
  $zip=[IO.Compression.ZipFile]::OpenRead($nativeJar);try{foreach($entry in $zip.Entries){if($entry.Name -like '*.dll'){[IO.Compression.ZipFileExtensions]::ExtractToFile($entry,(Join-Path $natives $entry.Name),$true)}}}finally{$zip.Dispose()}
  & $runtime "-Dorg.lwjgl.librarypath=$natives" -cp "$classes;$cp" video.RenderVerifyForge;CheckExit 'LWJGL2 render verification'
 }
 $manifest=Join-Path $root "build/$kind/MANIFEST.MF";@('Manifest-Version: 1.0','FMLCorePlugin: video.Plugin','') | Set-Content -Encoding ascii $manifest
 $output=Join-Path $dist "loadingvideo-$kind-1.12.2-0.2.0.jar"
 & jar cfm $output $manifest -C $classes video/Plugin.class -C $classes video/Transformer.class -C $classes video/Player.class;CheckExit 'Packaging'
 Write-Output "Built $output"
}
