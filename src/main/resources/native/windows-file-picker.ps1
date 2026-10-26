param([ValidateSet('file','folder')][string]$Mode='file',[string]$InitialDirectory,[string]$Title,[switch]$ValidateOnly)
$ErrorActionPreference='Stop'
[Console]::OutputEncoding=New-Object System.Text.UTF8Encoding($false)
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
namespace ErgoProxy.Native {
 [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)]
 public struct FilterSpec {
  [MarshalAs(UnmanagedType.LPWStr)] public string Name;
  [MarshalAs(UnmanagedType.LPWStr)] public string Spec;
 }
 [ComImport, Guid("43826D1E-E718-42EE-BC55-A1E261C37BFE"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
 public interface IShellItem {
  void BindToHandler(IntPtr context, ref Guid handler, ref Guid iid, out IntPtr result);
  void GetParent(out IShellItem parent);
  void GetDisplayName(uint kind, out IntPtr name);
  void GetAttributes(uint mask, out uint attributes);
  void Compare(IShellItem other, uint hint, out int order);
 }
 [ComImport, Guid("42F85136-DB7E-439C-85F1-E4075D135FC8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
 public interface IFileDialog {
  [PreserveSig] int Show(IntPtr owner);
  void SetFileTypes(uint count, [In, MarshalAs(UnmanagedType.LPArray, SizeParamIndex=0)] FilterSpec[] filters);
  void SetFileTypeIndex(uint index);
  void GetFileTypeIndex(out uint index);
  void Advise(IntPtr events, out uint cookie);
  void Unadvise(uint cookie);
  void SetOptions(uint options);
  void GetOptions(out uint options);
  void SetDefaultFolder(IShellItem folder);
  void SetFolder(IShellItem folder);
  void GetFolder(out IShellItem folder);
  void GetCurrentSelection(out IShellItem item);
  void SetFileName([MarshalAs(UnmanagedType.LPWStr)] string name);
  void GetFileName([MarshalAs(UnmanagedType.LPWStr)] out string name);
  void SetTitle([MarshalAs(UnmanagedType.LPWStr)] string title);
  void SetOkButtonLabel([MarshalAs(UnmanagedType.LPWStr)] string title);
  void SetFileNameLabel([MarshalAs(UnmanagedType.LPWStr)] string title);
  void GetResult(out IShellItem item);
  void AddPlace(IShellItem item, uint place);
  void SetDefaultExtension([MarshalAs(UnmanagedType.LPWStr)] string extension);
  void Close(int result);
  void SetClientGuid(ref Guid guid);
  void ClearClientData();
  void SetFilter(IntPtr filter);
 }
 public static class Picker {
  [DllImport("shell32.dll", CharSet=CharSet.Unicode, PreserveSig=false)]
  static extern void SHCreateItemFromParsingName(string path, IntPtr context, ref Guid iid, out IShellItem item);
  public static string Open(bool folder, string initial, string title, bool validateOnly) {
   object instance=Activator.CreateInstance(Type.GetTypeFromCLSID(new Guid("DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7")));
   IFileDialog dialog=(IFileDialog)instance;
   try {
    if(validateOnly) { return "WINDOWS_COMMON_DIALOG_READY"; }
    uint options=0x40|0x800|0x8|0x02000000;
    options |= folder ? 0x20u : 0x1000u;
    dialog.SetOptions(options);
    dialog.SetTitle(title);
    if(!folder) { dialog.SetFileTypes(1,new FilterSpec[]{new FilterSpec{Name="Video",Spec="*.mp4;*.m4v;*.mkv;*.webm;*.mov;*.avi;*.ts"}}); }
    if(System.IO.Directory.Exists(initial)) {
     IShellItem location;
     Guid shellItem=new Guid("43826D1E-E718-42EE-BC55-A1E261C37BFE");
     SHCreateItemFromParsingName(initial,IntPtr.Zero,ref shellItem,out location);
     try { dialog.SetFolder(location); } finally { Marshal.FinalReleaseComObject(location); }
    }
    int result=dialog.Show(IntPtr.Zero);
    if(result==unchecked((int)0x800704C7)) { return null; }
    if(result<0) { Marshal.ThrowExceptionForHR(result); }
    IShellItem selected;
    dialog.GetResult(out selected);
    try {
     IntPtr value;
     selected.GetDisplayName(0x80058000,out value);
     try { return Marshal.PtrToStringUni(value); } finally { Marshal.FreeCoTaskMem(value); }
    } finally { Marshal.FinalReleaseComObject(selected); }
   } finally { Marshal.FinalReleaseComObject(instance); }
  }
 }
}
"@
$result=[ErgoProxy.Native.Picker]::Open(($Mode -eq 'folder'),$InitialDirectory,$Title,$ValidateOnly.IsPresent)
if($null -ne $result) { [Console]::Write($result) }
