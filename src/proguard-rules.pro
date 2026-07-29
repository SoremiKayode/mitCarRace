# Add any ProGuard configurations specific to this
# extension here.

-keep public class chatinputbox.chatinputboxnew.ChatInputBoxNew {
    public *;
 }
-keeppackagenames gnu.kawa**, gnu.expr**

-optimizationpasses 4
-allowaccessmodification
-mergeinterfacesaggressively

-repackageclasses 'chatinputbox/chatinputboxnew/repack'
-flattenpackagehierarchy
-dontpreverify
