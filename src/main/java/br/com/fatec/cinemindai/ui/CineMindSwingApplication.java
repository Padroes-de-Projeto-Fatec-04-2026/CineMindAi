package br.com.fatec.cinemindai.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

/** Desktop chat for CineMindAi. Start the Spring Boot API before launching this class. */
public class CineMindSwingApplication {
    private static final Color BG = new Color(13, 15, 21), PANEL = new Color(20, 23, 31), CARD = new Color(27, 31, 41);
    private static final Color TEXT = new Color(239, 241, 247), MUTED = new Color(145, 151, 166), ACCENT = new Color(211, 168, 91);
    private final JFrame frame = new JFrame("CineMind · seu guia de cinema");
    private final JPanel messages = new JPanel();
    private final JPanel suggestions = new JPanel(new GridLayout(2, 2, 8, 8));
    private final JTextArea input = new JTextArea(2, 30);
    private final JLabel status = new JLabel("●  Conectando ao CineMind");
    private final JButton send = new JButton("Enviar  ➤");
    private final JButton approve = new JButton("Aprovar busca");
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final java.util.List<Conversation> conversations = new ArrayList<>();
    private Conversation active;

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new CineMindSwingApplication().show()); }

    private void show() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(960, 680)); frame.setSize(1180, 800); frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG); frame.setLayout(new BorderLayout());
        frame.add(sidebar(), BorderLayout.WEST); frame.add(mainPanel(), BorderLayout.CENTER);
        startConversation(); frame.setVisible(true); checkConnection();
    }

    private JPanel sidebar() {
        JPanel side = new JPanel(new BorderLayout(0, 20)); side.setPreferredSize(new Dimension(250, 0)); side.setBackground(PANEL);
        side.setBorder(new EmptyBorder(24, 18, 20, 18));
        JLabel brand = new JLabel("◉  CINE"); brand.setForeground(ACCENT); brand.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel sub = label("MIND", 20, TEXT, true); JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); logo.setOpaque(false); logo.add(brand); logo.add(sub);
        JPanel top = new JPanel(new BorderLayout(0, 18)); top.setOpaque(false); top.add(logo, BorderLayout.NORTH);
        JButton fresh = button("＋   Nova conversa", ACCENT, BG); fresh.setHorizontalAlignment(SwingConstants.LEFT); fresh.addActionListener(e -> startConversation()); top.add(fresh, BorderLayout.CENTER);
        side.add(top, BorderLayout.NORTH);
        JPanel history = new JPanel(); history.setOpaque(false); history.setLayout(new BoxLayout(history, BoxLayout.Y_AXIS));
        JScrollPane historyScroll = new JScrollPane(history); historyScroll.setBorder(null); historyScroll.getViewport().setOpaque(false); historyScroll.setOpaque(false);
        side.add(historyScroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(); footer.setOpaque(false); footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.add(label("CINEMIND AI", 11, ACCENT, true)); footer.add(Box.createVerticalStrut(5)); footer.add(label("Seu próximo filme começa aqui.", 11, MUTED, false));
        side.add(footer, BorderLayout.SOUTH);
        side.putClientProperty("history", history);
        return side;
    }

    private JPanel mainPanel() {
        JPanel main = new JPanel(new BorderLayout()); main.setBackground(BG); main.setBorder(new EmptyBorder(0, 30, 22, 30));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); header.setBorder(new EmptyBorder(23, 0, 18, 0));
        JPanel title = new JPanel(); title.setOpaque(false); title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(label("CINE MIND", 11, ACCENT, true)); title.add(Box.createVerticalStrut(5)); title.add(label("Seu próximo filme começa aqui", 20, TEXT, true));
        header.add(title, BorderLayout.WEST); status.setFont(new Font("SansSerif", Font.PLAIN, 12)); status.setForeground(MUTED); header.add(status, BorderLayout.EAST);
        main.add(header, BorderLayout.NORTH);
        messages.setOpaque(false); messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS)); messages.setBorder(new EmptyBorder(16, 4, 20, 4));
        JScrollPane scroll = new JScrollPane(messages); scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.getVerticalScrollBar().setUnitIncrement(18);
        main.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(); bottom.setOpaque(false); bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        suggestions.setOpaque(false); suggestions.setBorder(new EmptyBorder(0, 0, 18, 0)); bottom.add(suggestions);
        JPanel compose = new JPanel(new BorderLayout(12, 0)); compose.setBackground(CARD); compose.setBorder(new EmptyBorder(11, 14, 11, 12));
        input.setLineWrap(true); input.setWrapStyleWord(true); input.setFont(new Font("SansSerif", Font.PLAIN, 14)); input.setForeground(TEXT); input.setCaretColor(ACCENT); input.setBackground(CARD); input.setBorder(null);
        input.setText(""); input.setToolTipText("Escreva sobre o que você quer assistir...");
        compose.add(input, BorderLayout.CENTER); send.setFocusable(false); styleButton(send, ACCENT, BG); send.addActionListener(e -> sendMessage()); compose.add(send, BorderLayout.EAST);
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send"); input.getActionMap().put("send", new AbstractAction(){public void actionPerformed(ActionEvent e){sendMessage();}});
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8)); actionRow.setOpaque(false); approve.setVisible(false); styleButton(approve, new Color(77, 128, 104), TEXT); approve.addActionListener(e -> approvePlan()); actionRow.add(approve);
        bottom.add(compose); bottom.add(actionRow); main.add(bottom, BorderLayout.SOUTH); return main;
    }

    private void startConversation() {
        active = new Conversation(UUID.randomUUID().toString()); conversations.add(active); refreshHistory();
        messages.removeAll(); addWelcome(); configureSuggestions(); approve.setVisible(false); messages.revalidate(); messages.repaint();
    }
    private void addWelcome() {
        JPanel hero = new JPanel(); hero.setOpaque(false); hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS)); hero.setBorder(new EmptyBorder(58, 12, 28, 12));
        JLabel icon = label("✦", 35, ACCENT, true); icon.setAlignmentX(Component.LEFT_ALIGNMENT); hero.add(icon);
        JLabel greeting = label("Oi! Vamos encontrar seu próximo filme?", 25, TEXT, true); greeting.setAlignmentX(Component.LEFT_ALIGNMENT); hero.add(Box.createVerticalStrut(12)); hero.add(greeting);
        JLabel detail = label("Me conte seu clima, gênero ou um filme que você curte.", 14, MUTED, false); detail.setAlignmentX(Component.LEFT_ALIGNMENT); hero.add(Box.createVerticalStrut(8)); hero.add(detail);
        messages.add(hero); messages.add(Box.createVerticalGlue());
    }
    private void configureSuggestions() {
        suggestions.removeAll(); String[] prompts = {"Filmes de ficção científica para pensar", "Algo parecido com Interestelar", "Uma comédia leve para hoje", "Suspenses que prendem do começo ao fim"};
        for (String p : prompts) { JButton b = button(p, CARD, Color.BLACK); b.setFont(new Font("SansSerif", Font.PLAIN, 12)); b.setBorder(new EmptyBorder(9, 12, 9, 12)); b.addActionListener(e -> { input.setText(p); sendMessage(); }); suggestions.add(b); }
        suggestions.revalidate(); suggestions.repaint();
    }
    private void sendMessage() {
        String text = input.getText().trim(); if (text.isEmpty() || active == null) return;
        input.setText(""); appendBubble("Você", text, true); setBusy(true, "●  CineMind está pensando…");
        new SwingWorker<JsonNode, Void>() { protected JsonNode doInBackground() throws Exception {
            ObjectNode body = mapper.createObjectNode(); body.put("conversationId", active.id); body.put("message", text); body.put("plannerMode", "AUTO");
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl("/api/agent/chat"))).timeout(Duration.ofMinutes(3)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("A API respondeu com erro (" + response.statusCode() + ").");
            return mapper.readTree(response.body());
        } protected void done() { try { JsonNode r = get(); String reply = r.path("reply").asText("Não consegui preparar uma resposta agora."); appendBubble("CineMind", reply, false); String state = r.path("state").asText(""); boolean waiting = state.equalsIgnoreCase("AWAITING_HUMAN_APPROVAL"); approve.setVisible(waiting); setBusy(false, waiting ? "●  Aguardando sua aprovação" : "●  Pronto para conversar"); }
            catch (Exception ex) { appendBubble("CineMind", "Não consegui conectar ao agente. Confira se a API está rodando em " + baseUrl + ".", false); approve.setVisible(false); setBusy(false, "●  API indisponível"); } } }.execute();
    }
    private void approvePlan() {
        if (active == null) return; setBusy(true, "●  Executando sua busca…");
        new SwingWorker<JsonNode, Void>() { protected JsonNode doInBackground() throws Exception {
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl("/api/agent/chat/" + active.id + "/approve"))).timeout(Duration.ofMinutes(3)).POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString()); if (res.statusCode() < 200 || res.statusCode() >= 300) throw new IllegalStateException("Aprovação não concluída"); return mapper.readTree(res.body());
        } protected void done() { try { JsonNode r = get(); appendBubble("CineMind", r.path("reply").asText("Busca concluída."), false); approve.setVisible(false); setBusy(false, "●  Pronto para conversar"); } catch (Exception e) { appendBubble("CineMind", "Não foi possível aprovar agora. Tente novamente.", false); setBusy(false, "●  Erro na aprovação"); } } }.execute();
    }
    private String baseUrl = System.getProperty("cinemind.api", "http://localhost:8080");
    private String apiUrl(String path) { return baseUrl + path; }
    private void checkConnection() { new SwingWorker<Boolean, Void>() { protected Boolean doInBackground() throws Exception { HttpRequest r = HttpRequest.newBuilder(URI.create(apiUrl("/api/agent/chat"))).timeout(Duration.ofSeconds(3)).GET().build(); return client.send(r, HttpResponse.BodyHandlers.discarding()).statusCode() < 500; } protected void done(){ try { status.setText(get() ? "●  Agente conectado" : "●  API indisponível"); status.setForeground(get() ? new Color(104, 185, 139) : new Color(220, 120, 100)); } catch(Exception e){ status.setText("●  Inicie a API para conversar"); status.setForeground(new Color(220, 120, 100)); } } }.execute(); }
    private void appendBubble(String who, String text, boolean user) {
        if (active != null) { active.history.add(new String[]{who, text, Boolean.toString(user)}); if (user && active.title == null) active.title = text.length() > 25 ? text.substring(0, 25) + "…" : text; refreshHistory(); }
        renderBubble(who, text, user);
    }
    private void renderBubble(String who, String text, boolean user) {
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false); row.setBorder(new EmptyBorder(8, 8, 8, 8));
        JPanel bubble = new JPanel(new BorderLayout(0, 6)); bubble.setBackground(user ? new Color(54, 48, 39) : CARD); bubble.setBorder(new EmptyBorder(13, 16, 13, 16)); bubble.setMaximumSize(new Dimension(700, Integer.MAX_VALUE));
        JLabel name = label(who, 11, user ? ACCENT : new Color(124, 179, 151), true); JTextPane content = new JTextPane(); content.setEditable(false); content.setOpaque(false); content.setForeground(TEXT); content.setFont(new Font("SansSerif", Font.PLAIN, 14)); content.setText(text); content.setBorder(null);
        bubble.add(name, BorderLayout.NORTH); bubble.add(content, BorderLayout.CENTER); row.add(bubble, user ? BorderLayout.EAST : BorderLayout.WEST); messages.add(row); messages.add(Box.createVerticalStrut(2)); messages.revalidate(); messages.repaint();
        SwingUtilities.invokeLater(() -> { Container p = messages.getParent(); if (p instanceof JViewport v) v.setViewPosition(new Point(0, messages.getHeight())); });
    }
    private void refreshHistory() { JPanel h = (JPanel)((JPanel)frame.getContentPane().getComponent(0)).getClientProperty("history"); if (h == null) return; h.removeAll(); h.add(label("SUAS CONVERSAS", 11, MUTED, true)); for (Conversation c : conversations) { JButton b = button("◷   " + (c.title == null ? "Nova conversa" : c.title), PANEL, c == active ? ACCENT : MUTED); b.setHorizontalAlignment(SwingConstants.LEFT); b.setBorder(new EmptyBorder(10, 3, 10, 3)); b.addActionListener(e -> { active = c; messages.removeAll(); addWelcome(); for (String[] m : c.history) renderBubble(m[0],m[1],Boolean.parseBoolean(m[2])); refreshHistory(); }); h.add(Box.createVerticalStrut(4)); h.add(b); } h.revalidate(); h.repaint(); }
    private void setBusy(boolean busy, String text) { send.setEnabled(!busy); approve.setEnabled(!busy); status.setText(text); }
    private static JLabel label(String t, int size, Color color, boolean bold) { JLabel l = new JLabel(t); l.setForeground(color); l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size)); return l; }
    private static JButton button(String t, Color bg, Color fg) { JButton b = new JButton(t); styleButton(b,bg,fg); return b; }
    private static void styleButton(JButton b, Color bg, Color fg) { b.setBackground(bg); b.setForeground(fg); b.setFont(new Font("SansSerif", Font.BOLD, 12)); b.setFocusPainted(false); b.setBorder(new EmptyBorder(11,14,11,14)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
    private static class Conversation { final String id; String title; final java.util.List<String[]> history = new ArrayList<>(); Conversation(String id){this.id=id;} }
}
