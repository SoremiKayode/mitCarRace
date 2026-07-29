package chatinputbox.chatinputboxnew;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.PopupWindow;

import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.MediaUtil;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChatInputBox extends AndroidViewComponent {

    private final ComponentContainer container;
    private final LinearLayout root;
    private final LinearLayout titleBar;
    private final ImageView drawerToggleButton;
    private final TextView titleTextView;
    private final LinearLayout titleActions;
    private final ImageView topMenuButton;
    private final LinearLayout screenLayout;
    private final FrameLayout chatContainer;
    private final LinearLayout chatPane;
    private final LinearLayout drawer;
    private final View drawerScrim;
    private final ScrollView drawerScroll;
    private final LinearLayout conversationList;
    private final TextView newChatButton;
    private final ScrollView scrollView;
    private final LinearLayout messagesBox;
    private final LinearLayout inputBar;
    private final EditText editText;
    private final ImageView audioButton;
    private final ImageView readAloudButton;
    private final ImageView sendButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HashMap<String, JSONArray> drawerConversationMap = new HashMap<String, JSONArray>();

    private int backgroundColor = Color.rgb(32, 33, 35);
    private int messageBackgroundColor = Color.rgb(52, 53, 65);
    private int inputBackgroundColor = Color.rgb(64, 65, 79);
    private int borderColor = Color.rgb(86, 88, 105);
    private int textColor = Color.WHITE;
    private int hintColor = Color.rgb(170, 170, 170);
    private int buttonColor = Color.WHITE;
    private int codeBackgroundColor = Color.rgb(20, 20, 20);
    private int drawerBackgroundColor = Color.rgb(32, 33, 35);
    private int titleBarBackgroundColor = Color.rgb(30, 30, 30);
    private int cardBackgroundColor = Color.rgb(42, 43, 56);

    private int cornerRadiusDp = 24;
    private String hint = "Ask me anything";
    private String titleBarText = "CodeIgnite GPT";
    private int streamSectionDelayMs = 220;
    private String drawerOpenIconPath = "";
    private String drawerCloseIconPath = "";
    private boolean drawerExpanded = false;

    private String sendButtonImagePath = "";
    private String micButtonImagePath = "";
    private String readAloudButtonImagePath = "";
    private boolean lockSendWhileGenerating = true;
    private boolean showSendWhileGenerating = true;
    private boolean isGenerating = false;
    private String sendButtonBusyImagePath = "";
    private boolean showReadAloudButton = false;
    private boolean autoShowReadAloudWhenText = true;
    private String currentConversationId = "";
    private String currentConversationContent = "";
    private final ArrayList<MenuAction> topMenuActions = new ArrayList<MenuAction>();
    private final ArrayList<String> audioReadAloudListItems = new ArrayList<String>();
    private final ArrayList<String> audioReadAloudReturnValues = new ArrayList<String>();
    private String copyChatMenuIcon = "📋";
    private String newChatMenuIcon = "✨";
    private String translateMenuIcon = "🌐";
    private String topMenuIconPath = "";
    private String drawerItemSelectIcon = "✅";
    private String drawerItemDeleteIcon = "🗑";
    private PopupWindow audioReadAloudListPopup;
    private String lastSelectedAudioReadAloudItem = "";
    private final JSONArray conversationStateList = new JSONArray();
    private String lastUpsertedConversationTag = "";

    public ChatInputBox(ComponentContainer container) {
        super(container);
        this.container = container;

        root = new LinearLayout(container.$context());
        root.setOrientation(LinearLayout.VERTICAL);

        titleBar = new LinearLayout(container.$context());
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(12), dp(10), dp(12), dp(10));

        drawerToggleButton = makeImageButton();
        titleTextView = new TextView(container.$context());
        titleTextView.setTextSize(19);
        titleTextView.setTypeface(Typeface.DEFAULT_BOLD);
        titleTextView.setPadding(dp(10), 0, 0, 0);

        titleActions = new LinearLayout(container.$context());
        titleActions.setOrientation(LinearLayout.HORIZONTAL);
        titleActions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        topMenuButton = makeImageButton();

        titleBar.addView(drawerToggleButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        titleBar.addView(titleTextView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titleActions.addView(topMenuButton, new LinearLayout.LayoutParams(dp(46), dp(46)));
        titleBar.addView(titleActions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        screenLayout = new LinearLayout(container.$context());
        screenLayout.setOrientation(LinearLayout.HORIZONTAL);

        drawer = new LinearLayout(container.$context());
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setPadding(dp(8), dp(10), dp(8), dp(10));

        drawerScrim = new View(container.$context());
        drawerScrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetDrawerExpanded(false);
            }
        });

        newChatButton = new TextView(container.$context());
        newChatButton.setText("+ New chat");
        newChatButton.setTypeface(Typeface.DEFAULT_BOLD);
        newChatButton.setTextSize(14);
        newChatButton.setPadding(dp(12), dp(10), dp(12), dp(10));
        newChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewChatClicked();
            }
        });

        drawer.addView(newChatButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        drawerScroll = new ScrollView(container.$context());
        conversationList = new LinearLayout(container.$context());
        conversationList.setOrientation(LinearLayout.VERTICAL);
        drawerScroll.addView(conversationList);

        drawer.addView(drawerScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        chatContainer = new FrameLayout(container.$context());
        chatPane = new LinearLayout(container.$context());
        chatPane.setOrientation(LinearLayout.VERTICAL);
        chatPane.setPadding(dp(8), dp(8), dp(8), dp(8));

        scrollView = new ScrollView(container.$context());
        messagesBox = new LinearLayout(container.$context());
        messagesBox.setOrientation(LinearLayout.VERTICAL);
        messagesBox.setPadding(dp(4), dp(4), dp(4), dp(10));
        scrollView.addView(messagesBox);

        chatPane.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        inputBar = new LinearLayout(container.$context());
        inputBar.setOrientation(LinearLayout.HORIZONTAL);
        inputBar.setGravity(Gravity.CENTER_VERTICAL);
        inputBar.setPadding(dp(12), dp(8), dp(8), dp(8));

        editText = new EditText(container.$context());
        editText.setSingleLine(false);
        editText.setMinLines(1);
        editText.setMaxLines(5);
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setTextSize(16);
        editText.setPadding(0, 0, dp(8), 0);

        inputBar.addView(editText, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        audioButton = makeImageButton();
        readAloudButton = makeImageButton();
        sendButton = makeImageButton();

        drawerToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetDrawerExpanded(!drawerExpanded);
            }
        });

        topMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTopMenu(v);
            }
        });

        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AITextBoxClicked();
            }
        });

        inputBar.addView(audioButton, new LinearLayout.LayoutParams(dp(46), dp(46)));
        inputBar.addView(readAloudButton, new LinearLayout.LayoutParams(dp(46), dp(46)));
        inputBar.addView(sendButton, new LinearLayout.LayoutParams(dp(46), dp(46)));

        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAudioReadAloudList(v, "audio");
                AudioClicked();
            }
        });

        readAloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAudioReadAloudList(v, "readAloud");
                ReadAloudRequested();
            }
        });
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    dismissAudioReadAloudList();
                }
                return false;
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateReadAloudVisibility(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!sendButton.isEnabled()) return;
                if (lockSendWhileGenerating) {
                    isGenerating = true;
                    refreshSendButtonState();
                }
                SendClicked(editText.getText().toString());
            }
        });

        chatPane.addView(inputBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        chatContainer.addView(chatPane, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        chatContainer.addView(drawerScrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        FrameLayout.LayoutParams drawerLayoutParams = new FrameLayout.LayoutParams(
                getDrawerExpandedWidthPx(),
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        drawerLayoutParams.gravity = Gravity.START;
        chatContainer.addView(drawer, drawerLayoutParams);
        screenLayout.addView(chatContainer, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        root.addView(titleBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(screenLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        applyStyle();
        resetDefaultTopMenuActions();
        showWelcomeMessage();
        Width(ViewGroup.LayoutParams.MATCH_PARENT);
        Height(ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public View getView() { return root; }

    @SimpleFunction(description = "Adds the chat UI into an Arrangement component.")
    public void AddToArrangement(AndroidViewComponent arrangement) {
        ViewGroup parent = (ViewGroup) arrangement.getView();
        if (root.getParent() != null) ((ViewGroup) root.getParent()).removeView(root);
        parent.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @SimpleFunction(description = "Displays and formats an AI response section by section for more natural streaming.")
    public void DisplayAIMessage(String message) {
        messagesBox.setGravity(Gravity.NO_GRAVITY);
        final ArrayList<View> sections = renderMessage(message);
        for (int i = 0; i < sections.size(); i++) {
            final View section = sections.get(i);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    messagesBox.addView(section);
                    scrollToBottom();
                }
            }, (long) i * streamSectionDelayMs);
        }
        if (isGenerating) {
            long doneDelay = Math.max(0, (long) (sections.size() - 1) * streamSectionDelayMs + 60);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isGenerating = false;
                    refreshSendButtonState();
                }
            }, doneDelay);
        }
    }

    @SimpleFunction(description = "Tracks prompt/message history and renders it. Appends prompt + AI response until ResetConversationStateList is called.")
    public void DisplayAIMessageWithState(String message, String prompt, String listJson) {
        try {
            JSONArray mergedState = copyJsonArray(conversationStateList);
            JSONArray incoming = parseOrFallbackList(listJson, new JSONArray());
            if (mergedState.length() == 0 && incoming.length() > 0) {
                mergedState = copyJsonArray(incoming);
            }

            String safePrompt = prompt == null ? "" : prompt.trim();
            String safeMessage = message == null ? "" : message.trim();
            boolean hasPrompt = safePrompt.length() > 0;
            boolean hasMessage = safeMessage.length() > 0;

            if (hasPrompt && !hasMessage) {
                JSONObject item = new JSONObject();
                item.put("prompt", safePrompt);
                item.put("message", "");
                item.put("tag", generateConversationTagFromPrompt(safePrompt));
                item.put("datetime", nowIso());
                item.put("lastAssistantMessage", "");
                mergedState.put(item);
            } else if (!hasPrompt && hasMessage) {
                String previousAssistantTranscript = buildAssistantOnlyTranscript(mergedState);
                String resolvedMessage = extractIncrementalAssistantMessage(safeMessage, previousAssistantTranscript);
                int attachIndex = findFirstPendingPromptIndex(mergedState);
                if (attachIndex >= 0) {
                    JSONObject target = mergedState.optJSONObject(attachIndex);
                    target.put("message", resolvedMessage);
                    target.put("datetime", nowIso());
                    target.put("lastAssistantMessage", safeMessage);
                    if (target.optString("tag", "").trim().length() == 0) {
                        target.put("tag", generateConversationTagFromPrompt(target.optString("prompt", "")));
                    }
                } else {
                    JSONObject item = new JSONObject();
                    item.put("prompt", "");
                    item.put("message", resolvedMessage);
                    item.put("tag", "chat_" + System.currentTimeMillis());
                    item.put("datetime", nowIso());
                    item.put("lastAssistantMessage", safeMessage);
                    mergedState.put(item);
                }
            } else if (hasPrompt && hasMessage) {
                String previousAssistantTranscript = buildAssistantOnlyTranscript(mergedState);
                String resolvedMessage = extractIncrementalAssistantMessage(safeMessage, previousAssistantTranscript);
                JSONObject item = new JSONObject();
                item.put("prompt", safePrompt);
                item.put("message", resolvedMessage);
                item.put("tag", generateConversationTagFromPrompt(safePrompt));
                item.put("datetime", nowIso());
                item.put("lastAssistantMessage", safeMessage);
                mergedState.put(item);
            }

            syncState(mergedState);
            currentConversationContent = buildTranscriptFromState(conversationStateList);
            redrawConversationFromStateList();
        } catch (Exception e) {
            DisplayAIMessage(message == null ? "" : message);
        }
    }

    @SimpleFunction(description = "Populate drawer from TinyDB query/object map or array format. Supports legacy [title,content,*] and TinyDB upsert state-list payloads.")
    public void SetConversationsFromDictionaryList(String jsonList) {
        conversationList.removeAllViews();
        drawerConversationMap.clear();
        try {
            ArrayList<JSONObject> rows = new ArrayList<JSONObject>();
            String source = jsonList == null ? "" : jsonList.trim();
            if (source.startsWith("{")) {
                JSONObject map = new JSONObject(source);
                JSONArray names = map.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String key = names.optString(i, "").trim();
                        if (key.length() == 0) continue;
                        JSONObject item = new JSONObject();
                        item.put(key, map.opt(key));
                        rows.add(item);
                    }
                }
            } else {
                JSONArray list = new JSONArray(source);
                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.optJSONObject(i);
                    if (item != null && item.length() > 0) rows.add(item);
                }
            }
            Collections.sort(rows, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject a, JSONObject b) {
                    return extractConversationDate(b).compareTo(extractConversationDate(a));
                }
            });
            for (int i = 0; i < rows.size(); i++) {
                JSONObject item = rows.get(i);
                JSONArray names = item.names();
                if (names == null || names.length() == 0) continue;
                final String conversationId = names.optString(0, "").trim();
                if (conversationId.length() == 0) continue;
                JSONArray valueList = item.optJSONArray(conversationId);
                if (valueList == null) continue;
                final JSONArray conversationParts = valueList;
                drawerConversationMap.put(conversationId, conversationParts);
                String configuredTitle = resolveDrawerTitle(conversationId, conversationParts);
                final String drawerTitle = configuredTitle.length() == 0 ? conversationId : configuredTitle;
                TextView row = makeConversationRow(drawerTitle);
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openConversationFromParts(conversationId, conversationParts);
                        ConversationSelected(conversationId, conversationParts.toString());
                    }
                });
                row.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        showDrawerItemAction(row, conversationId, drawerTitle, conversationParts.optString(1, ""));
                        return true;
                    }
                });
                conversationList.addView(row);
            }
        } catch (Exception e) {
            DisplayAIMessage("Could not parse conversation dictionary list.");
        }
    }

    @SimpleFunction(description = "Backward compatible: set drawer titles only (newline separated).")
    public void SetConversations(String newlineSeparatedTitles) {
        conversationList.removeAllViews();
        drawerConversationMap.clear();
        String[] items = newlineSeparatedTitles.split("\\n");
        for (int i = items.length - 1; i >= 0; i--) {
            final String title = items[i].trim();
            if (title.length() == 0) continue;
            TextView row = makeConversationRow(title);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    JSONArray conversationParts = drawerConversationMap.get(title);
                    String content = conversationParts == null ? "" : conversationParts.optString(1, "");
                    if (content.length() > 0) {
                        ClearMessages();
                        DisplayAIMessage(content);
                    }
                    ConversationSelected(title, conversationParts == null ? "[]" : conversationParts.toString());
                }
            });
            conversationList.addView(row);
        }
    }



    private void openConversationFromParts(String conversationId, JSONArray conversationParts) {
        ClearMessages();
        currentConversationId = conversationId == null ? "" : conversationId;

        JSONArray parsedState = parseConversationStateList(conversationParts == null ? null : conversationParts.opt(2));
        if (parsedState.length() > 0) {
            syncState(parsedState);
            redrawConversationFromStateList();
            currentConversationContent = buildTranscriptFromState(conversationStateList);
            return;
        }

        String stateJson = conversationParts == null ? "" : conversationParts.optString(2, "").trim();
        if (stateJson.length() > 0 && loadConversationStateFromJson(stateJson)) {
            currentConversationContent = buildTranscriptFromState(conversationStateList);
            return;
        }
        String content = conversationParts == null ? "" : conversationParts.optString(1, "");
        currentConversationContent = content;
        if (content.length() > 0) DisplayAIMessage(content);
    }

    private boolean loadConversationStateFromJson(String stateJson) {
        try {
            JSONArray state = parseConversationStateList(stateJson);
            if (state.length() == 0) return false;
            syncState(state);
            redrawConversationFromStateList();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String buildTranscriptFromState(JSONArray state) {
        if (state == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < state.length(); i++) {
            JSONObject item = state.optJSONObject(i);
            if (item == null) continue;
            String prompt = item.optString("prompt", "").trim();
            String message = item.optString("message", "").trim();
            if (prompt.length() > 0) out.append("User: ").append(prompt).append("\n");
            if (message.length() > 0) out.append("Assistant: ").append(message).append("\n\n");
        }
        return out.toString().trim();
    }

    @SimpleFunction(description = "Extracts a title from AI text using first sentence/paragraph.")
    public String ExtractTitleFromAIText(String aiText) {
        if (aiText == null) return "";
        String clean = aiText.trim();
        if (clean.length() == 0) return "";
        int paragraphBreak = clean.indexOf("\n\n");
        int sentenceBreak = clean.indexOf(". ");
        int cut = -1;
        if (paragraphBreak >= 0 && sentenceBreak >= 0) cut = Math.min(paragraphBreak, sentenceBreak + 1);
        else if (paragraphBreak >= 0) cut = paragraphBreak;
        else if (sentenceBreak >= 0) cut = sentenceBreak + 1;
        String title = (cut > 0 ? clean.substring(0, cut) : clean).replace("\n", " ").trim();
        if (title.length() > 80) title = title.substring(0, 80).trim() + "...";
        return title;
    }

    @SimpleFunction(description = "Clears all AI messages.")
    public void ClearMessages() {
        handler.removeCallbacksAndMessages(null);
        messagesBox.removeAllViews();
    }
    @SimpleFunction(description = "Set whether AI is currently generating (controls send button state/visibility).")
    public void SetGenerating(boolean generating) { isGenerating = generating; refreshSendButtonState(); }

    private void showWelcomeMessage() {
        messagesBox.removeAllViews();
        TextView welcome = new TextView(container.$context());
        welcome.setText("Ask any Question");
        welcome.setTextSize(20);
        welcome.setTypeface(Typeface.DEFAULT_BOLD);
        welcome.setTextColor(textColor);
        welcome.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(12), 0, dp(12));
        welcome.setLayoutParams(lp);
        messagesBox.setGravity(Gravity.CENTER);
        messagesBox.addView(welcome);
    }

    private ArrayList<View> renderMessage(String message) {
        ArrayList<View> views = new ArrayList<View>();
        String[] lines = message.split("\\n");
        boolean inCode = false;
        StringBuilder code = new StringBuilder();
        StringBuilder paragraph = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().startsWith("```")) {
                if (!inCode) { addParagraphIfAny(views, paragraph); inCode = true; code.setLength(0); }
                else { views.add(makeCodeBlock(code.toString())); inCode = false; }
                continue;
            }
            if (inCode) { code.append(line).append("\n"); continue; }

            if (line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ") || line.startsWith("- ") || line.startsWith("* ") || isMarkdownImage(line) || isImageUrl(line.trim()) || line.trim().length() == 0) {
                addParagraphIfAny(views, paragraph);
                if (line.startsWith("# ")) views.add(makeText(line.substring(2), 24, true, true));
                else if (line.startsWith("## ")) views.add(makeText(line.substring(3), 21, true, true));
                else if (line.startsWith("### ")) views.add(makeText(line.substring(4), 18, true, true));
                else if (line.startsWith("- ") || line.startsWith("* ")) views.add(makeText("• " + line.substring(2), 16, false, true));
                else if (isMarkdownImage(line)) views.add(makeImage(extractMarkdownImageUrl(line)));
                else if (isImageUrl(line.trim())) views.add(makeImage(line.trim()));
                continue;
            }
            paragraph.append(line).append("\n");
        }
        if (inCode) views.add(makeCodeBlock(code.toString()));
        addParagraphIfAny(views, paragraph);
        return views;
    }

    private void addParagraphIfAny(ArrayList<View> views, StringBuilder paragraph) {
        String text = paragraph.toString().trim();
        if (text.length() > 0) { views.add(makeText(text, 16, false, true)); paragraph.setLength(0); }
    }

    private TextView makeText(String text, int size, boolean bold, boolean formatted) {
        TextView tv = new TextView(container.$context());
        tv.setText(formatted ? Html.fromHtml(formatInline(text)) : text);
        tv.setTextColor(textColor);
        tv.setTextSize(size);
        tv.setLineSpacing(dp(3), 1.0f);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        tv.setLayoutParams(lp);
        return tv;
    }


    private View makeUserPromptBubble(String promptText) {
        final String fullPrompt = promptText == null ? "" : promptText.trim();
        final String displayPrompt = fullPrompt;
        LinearLayout row = new LinearLayout(container.$context());
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(2), 0, dp(4));
        row.setLayoutParams(rowLp);
        row.setGravity(Gravity.END);

        LinearLayout bubble = new LinearLayout(container.$context());
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable bubbleBg = new GradientDrawable();
        bubbleBg.setColor(Color.rgb(64, 68, 88));
        bubbleBg.setCornerRadius(dp(19));
        bubble.setBackground(bubbleBg);

        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bubbleLp.gravity = Gravity.END;
        bubbleLp.setMargins(dp(42), 0, 0, 0);
        bubble.setLayoutParams(bubbleLp);

        TextView promptTextView = new TextView(container.$context());
        promptTextView.setText(displayPrompt);
        promptTextView.setTextColor(Color.rgb(238, 241, 248));
        promptTextView.setTextSize(15);
        promptTextView.setLineSpacing(dp(3), 1.0f);

        final TextView copyBtn = new TextView(container.$context());
        copyBtn.setText("Copy");
        copyBtn.setTextColor(Color.rgb(215, 220, 240));
        copyBtn.setTextSize(12);
        copyBtn.setPadding(0, dp(4), 0, 0);
        copyBtn.setGravity(Gravity.END);
        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) container.$context().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("prompt", fullPrompt));
                copyBtn.setText("Copied");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        copyBtn.setText("Copy");
                    }
                }, 1000);
            }
        });

        bubble.addView(promptTextView);
        bubble.addView(copyBtn);
        row.addView(bubble);
        return row;
    }

    private TextView makeConversationRow(String text) {
        TextView row = new TextView(container.$context());
        row.setText(text);
        row.setEllipsize(TextUtils.TruncateAt.END);
        row.setSingleLine(true);
        row.setTextSize(14);
        row.setTextColor(textColor);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardBackgroundColor);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(75, 76, 94));
        row.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        row.setLayoutParams(lp);
        return row;
    }

    private LinearLayout makeCodeBlock(final String codeText) {
        LinearLayout outer = new LinearLayout(container.$context());
        outer.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable outerBg = new GradientDrawable();
        outerBg.setColor(codeBackgroundColor);
        outerBg.setCornerRadius(dp(12));
        outer.setBackground(outerBg);
        outer.setPadding(dp(10), dp(8), dp(10), dp(10));
        LinearLayout header = new LinearLayout(container.$context());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        final TextView copy = new TextView(container.$context());
        copy.setText("Copy");
        copy.setTextColor(Color.rgb(180, 180, 180));
        copy.setTextSize(12);
        copy.setPadding(dp(8), dp(4), dp(8), dp(4));
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) container.$context().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("code", codeText));
                copy.setText("Copied");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        copy.setText("Copy");
                    }
                }, 1000);
            }
        });
        header.addView(copy);
        outer.addView(header);
        HorizontalScrollView hsv = new HorizontalScrollView(container.$context());
        TextView codeView = new TextView(container.$context());
        codeView.setText(highlightCode(codeText));
        codeView.setTextColor(Color.rgb(220, 220, 220));
        codeView.setTextSize(14);
        codeView.setTypeface(Typeface.MONOSPACE);
        codeView.setPadding(dp(12), dp(12), dp(12), dp(12));
        codeView.setTextIsSelectable(true);
        codeView.setBackgroundColor(Color.TRANSPARENT);
        hsv.addView(codeView);
        outer.addView(hsv);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        outer.setLayoutParams(lp);
        return outer;
    }
    private CharSequence highlightCode(String code) {
        SpannableString span = new SpannableString(code);

        applySpanByRegex(span, code, "(?m)#.*$|//.*$|--.*$|/\\*([\\s\\S]*?)\\*/", 0, Color.rgb(98, 114, 164));
        applySpanByRegex(span, code, "\"([^\\\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'", 0, Color.rgb(241, 250, 140));

        applySpanByRegex(span, code, "\\b(import|from|package|using|include|require|module|namespace|export)\\b", 0, Color.rgb(139, 233, 253));
        applySpanByRegex(span, code, "\\b(class|interface|enum|struct|record|trait|object|protocol|extends|implements)\\b", 0, Color.rgb(255, 121, 198));
        applySpanByRegex(span, code, "\\b(public|private|protected|internal|static|final|abstract|readonly|sealed|override|virtual|const|volatile|synchronized)\\b", 0, Color.rgb(255, 85, 85));
        applySpanByRegex(span, code, "\\b(if|else|switch|case|when|break|continue|return|try|catch|finally|throw|throws)\\b", 0, Color.rgb(189, 147, 249));
        applySpanByRegex(span, code, "\\b(for|while|do|foreach|in|of)\\b", 0, Color.rgb(255, 184, 108));
        applySpanByRegex(span, code, "\\b(async|await|yield|lambda|new|delete|typeof|instanceof)\\b", 0, Color.rgb(80, 250, 123));

        applySpanByRegex(span, code, "\\b(function|fun|def|fn|void|int|float|double|String|boolean|char|long|short|byte|var|let|val|const)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(", 2, Color.rgb(80, 250, 123));
        applySpanByRegex(span, code, "\\b(class|interface|enum|struct|record)\\s+([A-Za-z_][A-Za-z0-9_]*)", 2, Color.rgb(255, 121, 198));
        applySpanByRegex(span, code, "@[A-Za-z_][A-Za-z0-9_]*", 0, Color.rgb(255, 184, 108));

        applySpanByRegex(span, code, "\\b(true|false|null|None|undefined|NaN|Infinity|\\d+(?:\\.\\d+)?)\\b", 0, Color.rgb(241, 250, 140));
        applySpanByRegex(span, code, "\\b([A-Za-z_][A-Za-z0-9_]*)\\s*=", 1, Color.rgb(255, 121, 198));
        applySpanByRegex(span, code, "(==|!=|<=|>=|&&|\\|\\||=>|->|::|\\+\\+|--|[+\\-*/%])", 0, Color.rgb(139, 233, 253));
        applySpanByRegex(span, code, "[{}()\\[\\]]", 0, Color.rgb(98, 114, 164));
        applySpanByRegex(span, code, "(?i)\\b(SELECT|FROM|WHERE|JOIN|ORDER\\s+BY|GROUP\\s+BY|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)\\b", 0, Color.rgb(255, 85, 85));
        applySpanByRegex(span, code, "(?i)</?[a-z][a-z0-9-]*\\b[^>]*>", 0, Color.rgb(139, 233, 253));

        return span;
    }

    private void applySpanByRegex(SpannableString span, String code, String regex, int groupIndex, int color) {
        Matcher m = Pattern.compile(regex).matcher(code);
        while (m.find()) {
            int start = groupIndex == 0 ? m.start() : m.start(groupIndex);
            int end = groupIndex == 0 ? m.end() : m.end(groupIndex);
            if (start >= 0 && end > start) span.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private View makeImage(final String imageUrl) { /* unchanged behavior */
        final ImageView imageView = new ImageView(container.$context());
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(0, dp(8), 0, dp(8));
        imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream input = new URL(imageUrl).openStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);
                    handler.post(new Runnable() { @Override public void run() { imageView.setImageBitmap(bitmap); } });
                } catch (Exception e) {
                    handler.post(new Runnable() { @Override public void run() { messagesBox.addView(makeText("Image could not be loaded: " + imageUrl, 14, false, false)); } });
                }
            }
        }).start();
        return imageView;
    }

    private ImageView makeImageButton() {
        ImageView button = new ImageView(container.$context());
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(6), dp(6), dp(6), dp(6));
        return button;
    }

    private void setButtonAsset(ImageView view, String path, String fallbackGlyph) {
        try {
            if (path != null && path.trim().length() > 0) {
                Drawable drawable = MediaUtil.getBitmapDrawable(container.$form(), path);
                view.setImageDrawable(drawable);
                view.setBackgroundColor(Color.TRANSPARENT);
                return;
            }
        } catch (Exception ignored) {}
        view.setImageDrawable(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(84, 86, 102));
        bg.setShape(GradientDrawable.OVAL);
        view.setBackground(bg);
        view.setContentDescription(fallbackGlyph);
    }

    private String formatInline(String text) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        safe = safe.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        safe = safe.replaceAll("`(.*?)`", "<tt>$1</tt>");
        return safe.replace("\n", "<br>");
    }
    private boolean isMarkdownImage(String line) { return line.trim().startsWith("![") && line.contains("](") && line.endsWith(")"); }
    private String extractMarkdownImageUrl(String line) { int start = line.indexOf("]("); return line.substring(start + 2, line.length() - 1); }
    private boolean isImageUrl(String line) { String lower = line.toLowerCase(); return lower.startsWith("http") && (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp")); }




    private void redrawConversationFromStateList() {
        handler.removeCallbacksAndMessages(null);
        messagesBox.removeAllViews();
        messagesBox.setGravity(Gravity.NO_GRAVITY);

        for (int i = 0; i < conversationStateList.length(); i++) {
            JSONObject item = conversationStateList.optJSONObject(i);
            if (item == null) continue;

            String promptText = item.optString("prompt", "").trim();
            String responseText = item.optString("message", "").trim();

            if (promptText.length() > 0) {
                messagesBox.addView(makeUserPromptBubble(promptText));
            }

            if (responseText.length() > 0) {
                DisplayAIMessageImmediate(responseText);
            }
        }

        scrollToBottom();
    }

    private void DisplayAIMessageImmediate(String message) {
        ArrayList<View> sections = renderMessage(message);
        for (int i = 0; i < sections.size(); i++) {
            messagesBox.addView(sections.get(i));
        }
        scrollToBottom();
    }

    private void showDrawerItemAction(final TextView row, final String id, final String title, final String content) {
        final TextView action = new TextView(container.$context());
        action.setText(drawerItemSelectIcon + " Select");
        action.setTextColor(Color.WHITE);
        action.setTextSize(12);
        action.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(75, 120, 230));
        bg.setCornerRadius(dp(8));
        action.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        action.setLayoutParams(lp);
        final ViewGroup parent = (ViewGroup) row.getParent();
        int index = parent.indexOfChild(row);
        if (index + 1 < parent.getChildCount() && parent.getChildAt(index + 1) instanceof TextView) {
            parent.removeViewAt(index + 1);
        }
        final TextView deleteAction = new TextView(container.$context());
        deleteAction.setText(drawerItemDeleteIcon + " Delete");
        deleteAction.setTextColor(Color.WHITE);
        deleteAction.setTextSize(12);
        deleteAction.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable deleteBg = new GradientDrawable();
        deleteBg.setColor(Color.rgb(180, 55, 55));
        deleteBg.setCornerRadius(dp(8));
        deleteAction.setBackground(deleteBg);
        LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        deleteLp.gravity = Gravity.END;
        deleteLp.setMargins(0, dp(4), 0, 0);
        deleteAction.setLayoutParams(deleteLp);
        parent.addView(action, index + 1);
        parent.addView(deleteAction, index + 2);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerItemLongClicked(id, title, content);
                parent.removeView(action);
                parent.removeView(deleteAction);
            }
        });
        deleteAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.removeView(row);
                parent.removeView(action);
                parent.removeView(deleteAction);
                drawerConversationMap.remove(id);
                DrawerItemLongClicked(id, title, content);
            }
        });
    }

    private void CopyCurrentChatToClipboard() {
        StringBuilder chatText = new StringBuilder();
        for (int i = 0; i < messagesBox.getChildCount(); i++) {
            View child = messagesBox.getChildAt(i);
            if (child instanceof TextView) {
                String t = ((TextView) child).getText().toString().trim();
                if (t.length() > 0) chatText.append(t).append("\n\n");
            }
        }
        String data = chatText.toString().trim();
        CopyToClipboard("chat", data.length() == 0 ? "No chat content yet." : data);
        ChatCopied(data);
    }

    @SimpleFunction(description = "Copies text to the clipboard.")
    public void CopyToClipboard(String label, String content) {
        ClipboardManager clipboard = (ClipboardManager) container.$context().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, content));
        Toast.makeText(container.$context(), "Copied", Toast.LENGTH_SHORT).show();
    }

    @SimpleFunction(description = "Creates JSON for a drawer item with id, title and content.")
    public String BuildConversationItem(String id, String title, String content) {
        try {
            JSONObject root = new JSONObject();
            JSONArray values = new JSONArray();
            values.put(title == null ? "" : title);
            values.put(content == null ? "" : content);
            root.put(id == null ? "" : id, values);
            return root.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @SimpleFunction(description = "Returns current conversation state list JSON for persistence.")
    public String CurrentConversationListItem() {
        return conversationStateList.toString();
    }

    @SimpleFunction(description = "Builds drawer value list as [title, content, stateJson, datetime] from the active conversation state.")
    public String CurrentConversationDrawerValueList(String title) {
        try {
            JSONArray values = new JSONArray();
            String resolvedTitle = title == null ? "" : title.trim();
            if (resolvedTitle.length() == 0) resolvedTitle = ExtractTitleFromAIText(buildTranscriptFromState(conversationStateList));
            values.put(resolvedTitle);
            values.put(buildTranscriptFromState(conversationStateList));
            values.put(copyStateList(conversationStateList));
            values.put(nowIso());
            return values.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    @SimpleFunction(description = "Backward compatible alias for CurrentConversationListItem.")
    public String CurrentConversationStateItem() {
        return CurrentConversationListItem();
    }

    @SimpleFunction(description = "Resets tracked conversation state list.")
    public void ResetConversationStateList() {
        while (conversationStateList.length() > 0) {
            conversationStateList.remove(conversationStateList.length() - 1);
        }
    }

    @SimpleFunction(description = "Upserts full conversation list into TinyDB entries JSON. Uses current conversation id/tag when available.")
    public String UpsertConversationListForTinyDB(String tinyDbEntriesJson, String listJson) {
        JSONObject entries;
        try {
            String base = tinyDbEntriesJson == null ? "" : tinyDbEntriesJson.trim();
            entries = base.startsWith("{") ? new JSONObject(base) : new JSONObject();
        } catch (Exception e) {
            entries = new JSONObject();
        }

        JSONArray state = parseConversationStateList(listJson);
        if (state.length() == 0) state = copyStateList(conversationStateList);

        String tag = resolveConversationTag(state);

        try {
            entries.put(tag, state);
            lastUpsertedConversationTag = tag;
        } catch (Exception ignored) {}
        return entries.toString();
    }


    private JSONArray parseConversationStateList(Object source) {
        if (source == null || source == JSONObject.NULL) return new JSONArray();
        try {
            if (source instanceof JSONArray) return copyStateList((JSONArray) source);
            if (source instanceof JSONObject) {
                JSONObject obj = (JSONObject) source;
                JSONArray wrapped = obj.optJSONArray("state");
                if (wrapped != null) return copyStateList(wrapped);
                JSONArray arr = new JSONArray();
                arr.put(obj);
                return arr;
            }
            String safe = source.toString().trim();
            if (safe.length() == 0) return new JSONArray();
            if (safe.startsWith("[")) return new JSONArray(safe);
            if (safe.startsWith("{")) {
                JSONObject obj = new JSONObject(safe);
                JSONArray wrapped = obj.optJSONArray("state");
                if (wrapped != null) return wrapped;
                JSONArray arr = new JSONArray();
                arr.put(obj);
                return arr;
            }
        } catch (Exception ignored) {}
        return new JSONArray();
    }


    private JSONArray copyStateList(JSONArray source) {
        JSONArray copy = new JSONArray();
        if (source == null) return copy;
        for (int i = 0; i < source.length(); i++) copy.put(source.opt(i));
        return copy;
    }

    private String resolveConversationTag(JSONArray state) {
        String candidate = currentConversationId == null ? "" : currentConversationId.trim();
        if (candidate.length() == 0 && state != null) {
            for (int i = state.length() - 1; i >= 0; i--) {
                JSONObject item = state.optJSONObject(i);
                if (item == null) continue;
                candidate = item.optString("tag", "").trim();
                if (candidate.length() > 0) break;
            }
        }
        if (candidate.length() == 0) candidate = generateConversationTagFromList(state);
        return candidate;
    }

    @SimpleFunction(description = "Returns the tag used by the most recent UpsertConversationListForTinyDB call.")
    public String LastUpsertConversationTag() {
        return lastUpsertedConversationTag == null ? "" : lastUpsertedConversationTag;
    }

    private String generateConversationTagFromList(JSONArray list) {
        String candidate = "";
        int len = list == null ? 0 : list.length();
        if (len > 0) {
            JSONObject last = list.optJSONObject(len - 1);
            if (last != null) {
                candidate = last.optString("prompt", "").trim();
                if (candidate.length() == 0) candidate = last.optString("tag", "").trim();
            }
        }
        if (candidate.length() == 0) candidate = "chat_" + System.currentTimeMillis();
        candidate = candidate.replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_\\-]", "");
        if (candidate.length() > 64) candidate = candidate.substring(0, 64);
        if (candidate.length() == 0) candidate = "chat_" + System.currentTimeMillis();
        return candidate;
    }

    private String generateConversationTagFromPrompt(String prompt) {
        String candidate = prompt == null ? "" : prompt.trim();
        if (candidate.length() == 0) candidate = "chat_" + System.currentTimeMillis();
        candidate = candidate.replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_\\-]", "");
        if (candidate.length() > 64) candidate = candidate.substring(0, 64);
        if (candidate.length() == 0) candidate = "chat_" + System.currentTimeMillis();
        return candidate;
    }

    private String truncatePromptPreview(String prompt, int maxWords) {
        String safe = prompt == null ? "" : prompt.trim();
        if (safe.length() == 0) return safe;
        String[] words = safe.split("\\s+");
        if (words.length <= maxWords) return safe;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) out.append(" ");
            out.append(words[i]);
        }
        out.append(" …");
        return out.toString();
    }

    private String buildAssistantOnlyTranscript(JSONArray state) {
        StringBuilder sb = new StringBuilder();
        if (state == null) return "";

        for (int i = 0; i < state.length(); i++) {
            JSONObject item = state.optJSONObject(i);
            if (item == null) continue;

            String prompt = item.optString("prompt", "").trim();
            String message = item.optString("message", "").trim();
            if (prompt.length() > 0 && message.length() > 0) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(message);
            }
        }

        return sb.toString();
    }

    private JSONArray copyJsonArray(JSONArray source) {
        JSONArray copy = new JSONArray();
        if (source == null) return copy;
        for (int i = 0; i < source.length(); i++) {
            copy.put(source.opt(i));
        }
        return copy;
    }

    private int findFirstPendingPromptIndex(JSONArray state) {
        if (state == null) return -1;
        for (int i = 0; i < state.length(); i++) {
            JSONObject item = state.optJSONObject(i);
            if (item == null) continue;
            String prompt = item.optString("prompt", "").trim();
            String message = item.optString("message", "").trim();
            if (prompt.length() > 0 && message.length() == 0) {
                return i;
            }
        }
        return -1;
    }

    private String extractIncrementalAssistantMessage(String fullMessage, String previousAssistantTranscript) {
        if (fullMessage == null) return "";

        String full = normalizeAssistantText(fullMessage);
        String previous = normalizeAssistantText(previousAssistantTranscript);

        if (previous.length() == 0) return full.trim();

        if (full.startsWith(previous)) {
            return full.substring(previous.length()).trim();
        }

        int index = full.indexOf(previous);
        if (index >= 0) {
            return full.substring(index + previous.length()).trim();
        }

        return full.trim();
    }

    private String normalizeAssistantText(String text) {
        if (text == null) return "";
        return text.replace("\r\n", "\n").replace("\r", "\n").trim();
    }


    private JSONArray mergeStateLists(JSONArray base, JSONArray incoming) {
        JSONArray merged = new JSONArray();
        appendUniqueConversationItems(merged, base);
        appendUniqueConversationItems(merged, incoming);
        return merged;
    }

    private void appendUniqueConversationItems(JSONArray target, JSONArray source) {
        if (source == null) return;
        for (int i = 0; i < source.length(); i++) {
            JSONObject candidate = source.optJSONObject(i);
            if (candidate == null) continue;
            String prompt = candidate.optString("prompt", "").trim();
            String message = candidate.optString("message", "").trim();
            boolean duplicate = false;
            for (int j = 0; j < target.length(); j++) {
                JSONObject existing = target.optJSONObject(j);
                if (existing == null) continue;
                String existingPrompt = existing.optString("prompt", "").trim();
                String existingMessage = existing.optString("message", "").trim();
                if (prompt.equals(existingPrompt) && message.equals(existingMessage)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) target.put(candidate);
        }
    }

    private JSONArray parseOrFallbackList(String json, JSONArray fallback) {
        try {
            if (json != null && json.trim().startsWith("[")) return new JSONArray(json);
        } catch (Exception ignored) {}
        JSONArray copy = new JSONArray();
        for (int i = 0; i < fallback.length(); i++) copy.put(fallback.opt(i));
        return copy;
    }

    private void syncState(JSONArray state) {
        ResetConversationStateList();
        for (int i = 0; i < state.length(); i++) conversationStateList.put(state.opt(i));
    }

    private String extractConversationDate(JSONObject item) {
        try {
            JSONArray names = item.names();
            if (names == null || names.length() == 0) return "";
            String id = names.optString(0, "");
            JSONArray parts = item.optJSONArray(id);
            if (parts != null && parts.length() > 0) {
                if (parts.length() > 2) {
                    String legacyDate = parts.optString(2, "").trim();
                    if (legacyDate.length() > 0) return legacyDate;
                }
                String stateDate = extractLatestStateDate(parts);
                if (stateDate.length() > 0) return stateDate;
            }
            JSONObject root = item.optJSONObject(id);
            if (root != null) return root.optString("datetime", "");
        } catch (Exception ignored) {}
        return "";
    }

    private String resolveDrawerTitle(String conversationId, JSONArray conversationParts) {
        if (conversationParts == null || conversationParts.length() == 0) {
            return conversationId == null ? "" : conversationId;
        }

        JSONObject firstState = conversationParts.optJSONObject(0);
        if (firstState != null) {
            for (int i = conversationParts.length() - 1; i >= 0; i--) {
                JSONObject stateItem = conversationParts.optJSONObject(i);
                if (stateItem == null) continue;
                String prompt = stateItem.optString("prompt", "").trim();
                if (prompt.length() > 0) return truncatePromptPreview(prompt, 8);
            }
            return conversationId == null ? "" : conversationId;
        }

        String configuredTitle = conversationParts.optString(0, "").trim();
        if (configuredTitle.length() == 0) {
            configuredTitle = ExtractTitleFromAIText(conversationParts.optString(1, ""));
        }
        return configuredTitle;
    }

    private String extractLatestStateDate(JSONArray conversationParts) {
        if (conversationParts == null) return "";
        for (int i = conversationParts.length() - 1; i >= 0; i--) {
            JSONObject stateItem = conversationParts.optJSONObject(i);
            if (stateItem == null) continue;
            String value = stateItem.optString("datetime", "").trim();
            if (value.length() > 0) return value;
        }
        return "";
    }

    private String nowIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }

    @SimpleFunction(description = "Sets popup list items from dictionary JSON, list JSON, comma-separated string, or newline-separated string.")
    public void SetAudioReadAloudListItems(String rawItems) {
        audioReadAloudListItems.clear();
        audioReadAloudReturnValues.clear();
        if (rawItems == null) return;
        String text = rawItems.trim();
        if (text.length() == 0) return;

        boolean parsed = false;
        try {
            if (text.startsWith("{") && text.endsWith("}")) {
                JSONObject object = new JSONObject(text);
                JSONArray names = object.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String key = names.optString(i, "");
                        if (key == null) key = "";
                        key = key.trim();
                        if (key.length() == 0) continue;
                        String value = object.optString(key, "").trim();
                        audioReadAloudListItems.add(value);
                        audioReadAloudReturnValues.add(key);
                    }
                }
                parsed = true;
            } else if (text.startsWith("[") && text.endsWith("]")) {
                JSONArray array = new JSONArray(text);
                for (int i = 0; i < array.length(); i++) {
                    String value = String.valueOf(array.opt(i)).trim();
                    if (value.length() == 0 || "null".equalsIgnoreCase(value)) continue;
                    audioReadAloudListItems.add(value);
                    audioReadAloudReturnValues.add(value);
                }
                parsed = true;
            }
        } catch (Exception ignored) {
            parsed = false;
        }

        if (!parsed) {
            String[] parts = text.contains(",") ? text.split("\\s*,\\s*") : text.split("\\n");
            for (String part : parts) {
                String item = part == null ? "" : part.trim();
                if (item.length() == 0) continue;
                audioReadAloudListItems.add(item);
                audioReadAloudReturnValues.add(item);
            }
        }
        dismissAudioReadAloudList();
    }

    @SimpleFunction(description = "Returns the last selected item from the Audio/Read Aloud popup list.")
    public String SelectedAudioReadAloudItem() {
        return lastSelectedAudioReadAloudItem;
    }


    @SimpleFunction(description = "Returns a dictionary JSON of language short codes mapped to language names for MIT App Inventor TextToSpeech and SpeechRecognizer. Uses ISO language codes from the Android runtime locale set.")
    public String SupportedSpeechLanguageCodes() {
        try {
            TreeMap<String, String> map = new TreeMap<String, String>();
            String[] isoLanguages = Locale.getISOLanguages();
            for (String code : isoLanguages) {
                if (code == null) continue;
                String normalizedCode = code.trim().toLowerCase(Locale.US);
                if (normalizedCode.length() != 2) continue;
                Locale locale = new Locale(normalizedCode);
                String name = locale.getDisplayLanguage(Locale.ENGLISH);
                if (name == null || name.trim().length() == 0) continue;
                map.put(normalizedCode, name);
            }
            JSONObject object = new JSONObject();
            for (String key : map.keySet()) {
                object.put(key, map.get(key));
            }
            return object.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @SimpleFunction(description = "Returns AI textbox text cleaned for TextToSpeech: removes code fences, markdown headings, bullets and symbols.")
    public String ReadableTextFromInput() {
        return CleanTextForReadAloud(editText.getText().toString());
    }

    @SimpleFunction(description = "Cleans any text for TextToSpeech by removing markdown/code formatting and returning readable plain text.")
    public String CleanTextForReadAloud(String text) {
        if (text == null) return "";
        String clean = text;
        clean = clean.replaceAll("```[\\s\\S]*?```", " ");
        clean = clean.replaceAll("`([^`]*)`", "$1");
        clean = clean.replaceAll("(?m)^\\s*#{1,6}\\s*", "");
        clean = clean.replaceAll("(?m)^\\s*[-*+]\\s+", "");
        clean = clean.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        clean = clean.replaceAll("\\*(.*?)\\*", "$1");
        clean = clean.replaceAll("!\\[[^\\]]*\\]\\(([^)]+)\\)", " ");
        clean = clean.replaceAll("\\[[^\\]]*\\]\\(([^)]+)\\)", "$1");
        clean = clean.replaceAll("[*_#>`~]", " ");
        clean = clean.replaceAll("\\s+", " ").trim();
        return clean;
    }

    private void toggleAudioReadAloudList(View anchor, final String source) {
        if (audioReadAloudListItems.size() == 0) return;
        if (audioReadAloudListPopup != null && audioReadAloudListPopup.isShowing()) {
            audioReadAloudListPopup.dismiss();
            return;
        }
        LinearLayout menuRoot = new LinearLayout(container.$context());
        menuRoot.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(12, 12, 12));
        bg.setCornerRadius(dp(10));
        menuRoot.setBackground(bg);
        menuRoot.setPadding(dp(10), dp(10), dp(10), dp(10));

        for (int i = 0; i < audioReadAloudListItems.size(); i++) {
            final int index = i;
            final String label = audioReadAloudListItems.get(i);
            TextView item = new TextView(container.$context());
            item.setText("🔊  " + label);
            item.setTextColor(Color.WHITE);
            item.setTextSize(14);
            item.setPadding(dp(8), dp(10), dp(8), dp(10));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String selected = index < audioReadAloudReturnValues.size() ? audioReadAloudReturnValues.get(index) : label;
                    lastSelectedAudioReadAloudItem = selected;
                    AudioReadAloudListItemSelected(source, selected, index + 1);
                    dismissAudioReadAloudList();
                }
            });
            menuRoot.addView(item);
            if (i < audioReadAloudListItems.size() - 1) {
                View line = new View(container.$context());
                line.setBackgroundColor(Color.rgb(70, 70, 70));
                menuRoot.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }

        ScrollView scrollView = new ScrollView(container.$context());
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.addView(menuRoot, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int popupWidth = Math.max(dp(220), anchor.getWidth());
        int popupMaxHeight = dp(260);
        audioReadAloudListPopup = new PopupWindow(scrollView, popupWidth, popupMaxHeight, true);
        configurePopupWindow(audioReadAloudListPopup);
        showPopupBelowAnchor(audioReadAloudListPopup, anchor, 0, dp(6));
    }



    private void showPopupBelowAnchor(final PopupWindow popupWindow, final View anchor, final int xoff, final int yoff) {
        if (popupWindow == null || anchor == null) return;
        anchor.post(new Runnable() {
            @Override
            public void run() {
                if (!anchor.isAttachedToWindow()) return;

                int popupWidth = popupWindow.getWidth();
                if (popupWidth <= 0) {
                    popupWidth = Math.max(dp(200), anchor.getWidth());
                    popupWindow.setWidth(popupWidth);
                }

                popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
                popupWindow.showAsDropDown(anchor, xoff, yoff, Gravity.START);
            }
        });
    }

    private void configurePopupWindow(PopupWindow popupWindow) {
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setClippingEnabled(true);
        popupWindow.setElevation(dp(12));
        GradientDrawable popupBackground = new GradientDrawable();
        popupBackground.setColor(Color.rgb(12, 12, 12));
        popupBackground.setCornerRadius(dp(10));
        popupWindow.setBackgroundDrawable(popupBackground);
    }

    private void dismissAudioReadAloudList() {
        if (audioReadAloudListPopup != null && audioReadAloudListPopup.isShowing()) {
            audioReadAloudListPopup.dismiss();
        }
    }


    private void showTopMenu(View anchor) {
        final PopupWindow popupWindow = new PopupWindow(container.$context());
        LinearLayout menuRoot = new LinearLayout(container.$context());
        menuRoot.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(12, 12, 12));
        bg.setCornerRadius(dp(10));
        menuRoot.setBackground(bg);
        menuRoot.setPadding(dp(10), dp(10), dp(10), dp(10));
        for (int i = 0; i < topMenuActions.size(); i++) {
            final MenuAction action = topMenuActions.get(i);
            TextView item = new TextView(container.$context());
            item.setText(action.icon + "  " + action.label);
            item.setTextColor(Color.WHITE);
            item.setTextSize(14);
            item.setPadding(dp(8), dp(10), dp(8), dp(10));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popupWindow.dismiss();
                    handleTopMenuAction(action.id);
                }
            });
            menuRoot.addView(item);
            if (i < topMenuActions.size() - 1) {
                View line = new View(container.$context());
                line.setBackgroundColor(Color.rgb(70, 70, 70));
                menuRoot.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }
        popupWindow.setContentView(menuRoot);
        configurePopupWindow(popupWindow);
        popupWindow.setWidth(dp(200));
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        showPopupBelowAnchor(popupWindow, anchor, -dp(140), dp(6));
    }

    private void handleTopMenuAction(int itemId) {
        if (itemId == 1) {
            CopyCurrentChatToClipboard();
            TopMenuCopyClicked();
            return;
        }
        if (itemId == 2) {
            NewChatClicked();
            return;
        }
        if (itemId == 3) {
            TranslateRequested(currentConversationId, editText.getText().toString().trim().length() > 0 ? editText.getText().toString() : currentConversationContent);
            return;
        }
        TopMenuCustomItemClicked(itemId);
    }
    
    private void resetDefaultTopMenuActions() {
        topMenuActions.clear();
        topMenuActions.add(new MenuAction(1, "Copy chat", copyChatMenuIcon));
        topMenuActions.add(new MenuAction(2, "New chat", newChatMenuIcon));
        topMenuActions.add(new MenuAction(3, "Translate", translateMenuIcon));
    }

    private void updateReadAloudVisibility() {
        boolean hasText = editText.getText() != null && editText.getText().toString().trim().length() > 0;
        boolean visible = showReadAloudButton || (autoShowReadAloudWhenText && hasText);
        readAloudButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void applyStyle() {
        root.setBackgroundColor(backgroundColor);
        drawer.setBackgroundColor(drawerBackgroundColor);
        drawer.setElevation(dp(12));
        titleBar.setBackgroundColor(titleBarBackgroundColor);
        titleBar.setElevation(dp(3));
        titleTextView.setText(titleBarText);
        titleTextView.setTextColor(textColor);
        refreshDrawerToggleIcon();
        GradientDrawable newChatBg = new GradientDrawable();
        newChatBg.setColor(Color.rgb(64, 65, 79));
        newChatBg.setCornerRadius(dp(10));
        newChatButton.setBackground(newChatBg);
        newChatButton.setTextColor(textColor);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(inputBackgroundColor);
        inputBg.setCornerRadius(dp(cornerRadiusDp));
        inputBg.setStroke(dp(1), borderColor);
        inputBar.setBackground(inputBg);
        editText.setHint(hint);
        editText.setTextColor(textColor);
        editText.setHintTextColor(hintColor);

        setButtonAsset(sendButton, sendButtonImagePath, "Send");
        setButtonAsset(audioButton, micButtonImagePath, "Mic");
        setButtonAsset(topMenuButton, topMenuIconPath, "⋮");
        setButtonAsset(readAloudButton, readAloudButtonImagePath, "🔊");
        updateReadAloudVisibility();
        refreshSendButtonState();
        updateDrawerLayoutWidth();
    }

    private int getDrawerExpandedWidthPx() {
        return Math.max(dp(180), Math.round(container.$context().getResources().getDisplayMetrics().widthPixels * 0.8f));
    }

    private void refreshDrawerToggleIcon() {
        String fallback = drawerExpanded ? "Collapse" : "Expand";
        String iconPath = drawerExpanded ? drawerCloseIconPath : drawerOpenIconPath;
        setButtonAsset(drawerToggleButton, iconPath, fallback);
    }

    private void updateDrawerLayoutWidth() {
        ViewGroup.LayoutParams lp = drawer.getLayoutParams();
        if (!(lp instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
        params.width = getDrawerExpandedWidthPx();
        drawer.setLayoutParams(params);
        drawer.setVisibility(drawerExpanded ? View.VISIBLE : View.GONE);
        drawerScrim.setVisibility(drawerExpanded ? View.VISIBLE : View.GONE);
        refreshDrawerToggleIcon();
    }

    @SimpleFunction(description = "Opens or collapses the drawer. Open = 80% screen width. Collapsed = hidden.")
    public void SetDrawerExpanded(boolean expanded) {
        drawerExpanded = expanded;
        updateDrawerLayoutWidth();
    }

    @SimpleProperty(description = "Returns true if the drawer is expanded.")
    public boolean DrawerExpanded() { return drawerExpanded; }

    private void refreshSendButtonState() {
        boolean enabled = !(lockSendWhileGenerating && isGenerating);
        sendButton.setEnabled(enabled);
        sendButton.setClickable(enabled);
        sendButton.setAlpha(enabled ? 1.0f : 0.45f);
        sendButton.setVisibility((isGenerating && !showSendWhileGenerating) ? View.GONE : View.VISIBLE);
        if (isGenerating && sendButtonBusyImagePath != null && sendButtonBusyImagePath.trim().length() > 0) {
            setButtonAsset(sendButton, sendButtonBusyImagePath, "Generating");
        } else {
            setButtonAsset(sendButton, sendButtonImagePath, "Send");
        }
    }

    private void scrollToBottom() { scrollView.post(new Runnable() { @Override public void run() { scrollView.fullScroll(View.FOCUS_DOWN); } }); }
    private int dp(int value) { return Math.round(value * container.$context().getResources().getDisplayMetrics().density); }

    @SimpleEvent(description = "Triggered when the send button is clicked. Returns the current prompt text.")
    public void SendClicked(String prompt) { EventDispatcher.dispatchEvent(this, "SendClicked", prompt); }
    @SimpleEvent(description = "Triggered when the audio button is clicked.")
    public void AudioClicked() { EventDispatcher.dispatchEvent(this, "AudioClicked"); }
    @SimpleEvent(description = "Triggered when user picks a conversation in the left drawer. Returns conversation id and value list JSON.")
    public void ConversationSelected(String conversationId, String valueListJson) {
        EventDispatcher.dispatchEvent(this, "ConversationSelected", conversationId, valueListJson);
    }
    @SimpleEvent(description = "Triggered when user taps new chat button in the drawer.")
    public void NewChatClicked() { EventDispatcher.dispatchEvent(this, "NewChatClicked"); }
    @SimpleEvent(description = "Triggered when copy chat action is clicked.")
    public void ChatCopied(String content) { EventDispatcher.dispatchEvent(this, "ChatCopied", content); }
    @SimpleEvent(description = "Triggered when copy action is clicked from the top menu.")
    public void TopMenuCopyClicked() { EventDispatcher.dispatchEvent(this, "TopMenuCopyClicked"); }
    @SimpleEvent(description = "Triggered when translate menu option is clicked. Returns code/id and content.")
    public void TranslateRequested(String code, String content) { EventDispatcher.dispatchEvent(this, "TranslateRequested", code, content); }
    @SimpleEvent(description = "Triggered when read aloud icon is clicked to request text for TTS.")
    public void ReadAloudRequested() { EventDispatcher.dispatchEvent(this, "ReadAloudRequested"); }
    @SimpleEvent(description = "Triggered when a drawer item action is selected after long click.")
    public void DrawerItemLongClicked(String conversationId, String title, String content) {
        EventDispatcher.dispatchEvent(this, "DrawerItemLongClicked", conversationId, title, content);
    }
    @SimpleEvent(description = "Triggered when AI text box is tapped.")
    public void AITextBoxClicked() { EventDispatcher.dispatchEvent(this, "AITextBoxClicked"); }
    @SimpleEvent(description = "Triggered when an item from the Audio/Read Aloud popup list is selected. Returns source button, selected item, and 1-based index.")
    public void AudioReadAloudListItemSelected(String source, String item, int index) {
        EventDispatcher.dispatchEvent(this, "AudioReadAloudListItemSelected", source, item, index);
    }
    @SimpleEvent(description = "Triggered when a custom title bar menu item is clicked. Returns item id.")
    public void TopMenuCustomItemClicked(int itemId) { EventDispatcher.dispatchEvent(this, "TopMenuCustomItemClicked", itemId); }

    @SimpleFunction(description = "Returns the current text inside the input box.")
    public String Text() { return editText.getText().toString(); }
    @SimpleFunction(description = "Sets the text inside the input box.")
    public void SetText(String text) { editText.setText(text); }
    @SimpleFunction(description = "Clears the input box.")
    public void Clear() { editText.setText(""); }
    @SimpleFunction(description = "Returns the text inside the chat input box.")
    public String GetChatInputText() { return Text(); }
    @SimpleFunction(description = "Sets the text inside the chat input box.")
    public void SetChatInputText(String text) { SetText(text); }
    @SimpleFunction(description = "Clears the chat input box.")
    public void ClearChatInputText() { Clear(); }

    @SimpleProperty(description = "Sets image asset path for send button. Example: send.png")
    public void SendButtonImage(String value) { sendButtonImagePath = value; applyStyle(); }
    @SimpleProperty(description = "Sets image asset path for send button while AI is generating.")
    public void SendButtonBusyImage(String value) { sendButtonBusyImagePath = value; applyStyle(); }
    @SimpleProperty(description = "Sets image asset path for microphone button. Example: mic.png")
    public void MicButtonImage(String value) { micButtonImagePath = value; applyStyle(); }
    @SimpleProperty(description = "Sets image asset path for read aloud button. Example: readaloud.png")
    public void ReadAloudButtonImage(String value) { readAloudButtonImagePath = value; applyStyle(); }
    @SimpleProperty(description = "Returns image asset path for read aloud button.")
    public String ReadAloudButtonImage() { return readAloudButtonImagePath; }
    @SimpleProperty(description = "Sets image asset path for title bar icon when drawer is closed (open icon).")
    public void DrawerOpenIconImage(String value) { drawerOpenIconPath = value; applyStyle(); }
    @SimpleProperty(description = "Sets image asset path for title bar icon when drawer is open (collapse icon).")
    public void DrawerCollapseIconImage(String value) { drawerCloseIconPath = value; applyStyle(); }
    @SimpleProperty(description = "Sets the title text displayed in the top title bar.")
    public void TitleBarText(String value) { titleBarText = value; applyStyle(); }
    @SimpleProperty(description = "Returns the current title bar text.")
    public String TitleBarText() { return titleBarText; }
    @SimpleProperty(description = "If true, send button becomes disabled while AI is generating.")
    public void DisableSendWhileGenerating(boolean value) { lockSendWhileGenerating = value; refreshSendButtonState(); }
    @SimpleProperty(description = "If true, send button is shown while AI is generating. If false it is hidden.")
    public void ShowSendWhileGenerating(boolean value) { showSendWhileGenerating = value; refreshSendButtonState(); }
    @SimpleProperty(description = "Controls whether read aloud button is always visible.")
    public void ShowReadAloudButton(boolean value) { showReadAloudButton = value; updateReadAloudVisibility(); }
    @SimpleProperty(description = "If true, read aloud button appears when AI textbox has content.")
    public void AutoShowReadAloudWhenText(boolean value) { autoShowReadAloudWhenText = value; updateReadAloudVisibility(); }
    @SimpleProperty(description = "Icon text for Copy chat item in title bar menu.")
    public void CopyChatMenuIcon(String value) { copyChatMenuIcon = value == null ? "" : value; resetDefaultTopMenuActions(); }
    @SimpleProperty(description = "Icon text for New chat item in title bar menu.")
    public void NewChatMenuIcon(String value) { newChatMenuIcon = value == null ? "" : value; resetDefaultTopMenuActions(); }
    @SimpleProperty(description = "Icon text for Translate item in title bar menu.")
    public void TranslateMenuIcon(String value) { translateMenuIcon = value == null ? "" : value; resetDefaultTopMenuActions(); }
    @SimpleProperty(description = "Sets image asset path for title bar menu icon.")
    public void TopMenuIconImage(String value) { topMenuIconPath = value; applyStyle(); }
    @SimpleProperty(description = "Sets icon text for drawer long-click select action.")
    public void DrawerItemSelectIcon(String value) { drawerItemSelectIcon = value == null ? "✅" : value; }
    @SimpleProperty(description = "Sets icon text for drawer long-click delete action.")
    public void DrawerItemDeleteIcon(String value) { drawerItemDeleteIcon = value == null ? "🗑" : value; }
    @SimpleFunction(description = "Adds a custom menu item to title bar menu.")
    public void AddTitleBarMenuItem(int id, String label, String iconText) { topMenuActions.add(new MenuAction(id, label, iconText)); }
    @SimpleProperty(description = "Sets the hint text shown when the input is empty.")
    public void Hint(String value) { hint = value; applyStyle(); }
    @SimpleProperty(description = "Returns the hint text.")
    public String Hint() { return hint; }
    @SimpleProperty(description = "Sets section streaming delay in milliseconds.")
    public void StreamSectionDelay(int value) { streamSectionDelayMs = value; }
    @SimpleProperty(description = "Sets the dark background color.")
    public void BackgroundColor(int value) { backgroundColor = value; applyStyle(); }
    @SimpleProperty(description = "Sets top title bar background color.")
    public void TitleBarBackgroundColor(int value) { titleBarBackgroundColor = value; applyStyle(); }
    @SimpleProperty(description = "Sets the AI message background color.")
    public void MessageBackgroundColor(int value) { messageBackgroundColor = value; }
    @SimpleProperty(description = "Sets the input bar background color.")
    public void InputBackgroundColor(int value) { inputBackgroundColor = value; applyStyle(); }
    @SimpleProperty(description = "Sets the border color of the input box.")
    public void BorderColor(int value) { borderColor = value; applyStyle(); }
    @SimpleProperty(description = "Sets the text color.")
    public void TextColor(int value) { textColor = value; applyStyle(); }
    @SimpleProperty(description = "Sets the hint text color.")
    public void HintColor(int value) { hintColor = value; applyStyle(); }
    @SimpleProperty(description = "Sets the corner radius in dp.")
    public void CornerRadius(int value) { cornerRadiusDp = value; applyStyle(); }

    private static class MenuAction {
        final int id;
        final String label;
        final String icon;
        MenuAction(int id, String label, String icon) {
            this.id = id;
            this.label = label == null ? "" : label;
            this.icon = icon == null ? "" : icon;
        }
    }
}
