package com.AugustBurns;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Side panel for the Collection Log Wiki plugin.
 * Provides manual NPC lookup and displays drop table results.
 */
public class CollectionPluginPanel extends PluginPanel
{
    private final JTextField searchField;
    private final JPanel resultsPanel;
    private final JLabel statusLabel;
    private Runnable onSearch;

    public CollectionPluginPanel()
    {
        setLayout(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Title
        JLabel titleLabel = new JLabel("Collection Log Expanded");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(new Color(255, 152, 31));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleLabel.getPreferredSize().height));

        // Search area
        JPanel searchPanel = new JPanel(new BorderLayout(4, 0));
        searchPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel searchLabel = new JLabel("Lookup NPC:");
        searchLabel.setForeground(Color.WHITE);
        searchLabel.setFont(FontManager.getRunescapeSmallFont());

        searchField = new JTextField();
        searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(4, 6, 4, 6)
        ));
        searchField.addActionListener(e -> doSearch());

        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(60, 50, 38));
        searchButton.setForeground(new Color(255, 203, 5));
        searchButton.setFocusPainted(false);
        searchButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(109, 96, 73)),
            new EmptyBorder(4, 10, 4, 10)
        ));
        searchButton.addActionListener(e -> doSearch());

        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        inputRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        inputRow.add(searchField, BorderLayout.CENTER);
        inputRow.add(searchButton, BorderLayout.EAST);

        searchPanel.add(searchLabel, BorderLayout.NORTH);
        searchPanel.add(inputRow, BorderLayout.SOUTH);

        // Status label
        statusLabel = new JLabel("Right-click an NPC in-game for drop info.");
        statusLabel.setFont(FontManager.getRunescapeSmallFont());
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setBorder(new EmptyBorder(4, 0, 4, 0));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, statusLabel.getPreferredSize().height));

        // Results area
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Top section
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topPanel.add(titleLabel);
        topPanel.add(Box.createVerticalStrut(6));
        topPanel.add(searchPanel);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Sets the callback for when a search is performed from the panel.
     */
    public void setSearchCallback(Runnable callback)
    {
        this.onSearch = callback;
    }

    /**
     * Returns the current search field text.
     */
    public String getSearchText()
    {
        return searchField.getText().trim();
    }

    /**
     * Sets the status text shown below the search bar.
     */
    public void setStatus(String text)
    {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    /**
     * Displays drop data in the side panel.
     */
    public void displayDropData(NpcDropData data)
    {
        SwingUtilities.invokeLater(() ->
        {
            resultsPanel.removeAll();

            if (data == null || data.getSections().isEmpty())
            {
                JLabel noData = new JLabel("No drops found.");
                noData.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                noData.setFont(FontManager.getRunescapeSmallFont());
                noData.setAlignmentX(Component.LEFT_ALIGNMENT);
                resultsPanel.add(noData);
                resultsPanel.revalidate();
                resultsPanel.repaint();
                return;
            }

            statusLabel.setText("Showing drops for: " + data.getNpcName());

            for (NpcDropData.DropSection section : data.getSections())
            {
                // Section header
                JLabel sectionLabel = new JLabel(section.getName() + " (" + section.getItems().size() + ")");
                sectionLabel.setFont(FontManager.getRunescapeBoldFont());
                sectionLabel.setForeground(new Color(255, 203, 5));
                sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                sectionLabel.setBorder(new EmptyBorder(6, 0, 2, 0));
                resultsPanel.add(sectionLabel);

                // Drop items
                for (NpcDropData.DropItem item : section.getItems())
                {
                    JPanel itemRow = createItemRow(item);
                    itemRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                    resultsPanel.add(itemRow);
                }

                // Separator
                JSeparator separator = new JSeparator();
                separator.setForeground(new Color(80, 70, 55));
                separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                resultsPanel.add(separator);
            }

            // Total count
            JLabel totalLabel = new JLabel(data.getTotalDropCount() + " unique drops");
            totalLabel.setFont(FontManager.getRunescapeSmallFont());
            totalLabel.setForeground(new Color(140, 130, 110));
            totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            totalLabel.setBorder(new EmptyBorder(6, 0, 0, 0));
            resultsPanel.add(totalLabel);

            resultsPanel.revalidate();
            resultsPanel.repaint();
        });
    }

    /**
     * Clears the results panel.
     */
    public void clearResults()
    {
        SwingUtilities.invokeLater(() ->
        {
            resultsPanel.removeAll();
            resultsPanel.revalidate();
            resultsPanel.repaint();
        });
    }

    private JPanel createItemRow(NpcDropData.DropItem item)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row.setBorder(new EmptyBorder(1, 8, 1, 4));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        String displayText = item.getDisplayName();
        if (item.getObtainedCount() > 0)
        {
            displayText += " [x" + item.getObtainedCount() + "]";
        }
        JLabel nameLabel = new JLabel(displayText);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setForeground(item.getObtainedCount() > 0 ? new Color(30, 200, 80) : new Color(225, 215, 195));

        JLabel rateLabel = new JLabel(item.getRarity());
        rateLabel.setFont(FontManager.getRunescapeSmallFont());
        rateLabel.setForeground(getRarityColor(item.getRarity()));
        rateLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(nameLabel, BorderLayout.CENTER);
        row.add(rateLabel, BorderLayout.EAST);

        return row;
    }

    private Color getRarityColor(String rarity)
    {
        if (rarity == null) return new Color(200, 190, 170);

        String lower = rarity.toLowerCase();
        if (lower.equals("always")) return new Color(30, 200, 80);
        if (lower.equals("common")) return new Color(180, 180, 180);
        if (lower.equals("uncommon")) return new Color(80, 190, 255);
        if (lower.equals("rare")) return new Color(180, 80, 255);
        if (lower.equals("very rare")) return new Color(255, 165, 50);

        if (rarity.contains("/"))
        {
            try
            {
                String[] parts = rarity.split("/");
                double rate = Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
                if (rate >= 1.0) return new Color(30, 200, 80);
                if (rate >= 0.05) return new Color(180, 180, 180);
                if (rate >= 0.01) return new Color(80, 190, 255);
                if (rate >= 0.002) return new Color(180, 80, 255);
                return new Color(255, 165, 50);
            }
            catch (NumberFormatException ignored) {}
        }
        return new Color(200, 190, 170);
    }

    private void doSearch()
    {
        if (onSearch != null)
        {
            onSearch.run();
        }
    }
}
