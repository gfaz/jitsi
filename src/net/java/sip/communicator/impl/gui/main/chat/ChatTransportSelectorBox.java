/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ChatTransportSelectorBox</tt> represents the send via menu in the
 * chat window. The menu contains all protocol specific transports. In the case
 * of meta contact these would be all contacts for the currently selected meta
 * contact chat.
 * 
 * @author Yana Stamcheva
 */
public class ChatTransportSelectorBox
    extends JMenuBar
    implements ActionListener
{
    private static final Logger logger
        = Logger.getLogger(ChatTransportSelectorBox.class);

    private Hashtable transportMenuItems = new Hashtable();

    private SIPCommMenu menu = new SIPCommMenu();

    private ChatPanel chatPanel;

    private ChatSession chatSession;

    public ChatTransportSelectorBox(ChatPanel chatPanel,
                                    ChatSession chatSession,
                                    ChatTransport selectedChatTransport)
    {
        this.chatPanel = chatPanel;

        this.chatSession = chatSession;

        this.menu.setPreferredSize(new Dimension(28, 24));
        this.menu.setUI(new SIPCommSelectorMenuUI());

        this.add(menu);

        Iterator chatTransports = chatSession.getChatTransports();
        while (chatTransports.hasNext())
        {
            ChatTransport chatTransport = (ChatTransport) chatTransports.next();

            this.addChatTransport(chatTransport);
        }

        this.setSelected(selectedChatTransport);
    }

    /**
     * Adds the given chat transport to the "send via" menu.
     * 
     * @param chatTransport The chat transport to add.
     */
    public void addChatTransport(ChatTransport chatTransport)
    {
        Image img = createTransportStatusImage(chatTransport);

        JMenuItem menuItem = new JMenuItem(
                    chatTransport.getName(),
                    new ImageIcon(img));

        menuItem.addActionListener(this);
        this.transportMenuItems.put(chatTransport, menuItem);

        this.menu.add(menuItem);
    }
    
    /**
     * Removes the given chat transport from the "send via" menu. This method is
     * used to update the "send via" menu when a protocol contact is moved or
     * removed from the contact list.
     * 
     * @param contact the proto contact to be removed
     */
    public void removeChatTransport(ChatTransport chatTransport)
    {
        this.menu.remove((JMenuItem)transportMenuItems.get(chatTransport));
        this.transportMenuItems.remove(chatTransport);
    }
    
    /**
     * The listener of the chat transport selector box.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        Enumeration<ChatTransport> chatTransports = transportMenuItems.keys();
        while(chatTransports.hasMoreElements())
        {
            ChatTransport chatTransport = chatTransports.nextElement();

            if (transportMenuItems.get(chatTransport).equals(menuItem))
            {
                this.setSelected(chatTransport, (ImageIcon) menuItem.getIcon());

                return;
            }
        }

        logger.debug( "Could not find contact for menu item "
                      + menuItem.getText() + ". contactsTable("
                      + transportMenuItems.size()+") is : "
                      + transportMenuItems);
    }

    /**
     * Obtains the status icon for the given chat transport and
     * adds to it the account index information.
     * 
     * @param chatTransport The chat transport for which to create the image.
     * @return The indexed status image.
     */
    public Image createTransportStatusImage(ChatTransport chatTransport)
    {
        Image statusImage = ImageLoader.getBytesInImage(
                chatTransport.getStatus().getStatusIcon());

        int index = GuiActivator.getUIService().getMainFrame()
            .getProviderIndex(chatTransport.getProtocolProvider());

        Image img = null;
        if(index > 0) {
            BufferedImage buffImage = new BufferedImage(
                    22, 16, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = (Graphics2D)buffImage.getGraphics();
            AlphaComposite ac =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.DARK_GRAY);
            g.setFont(Constants.FONT.deriveFont(Font.BOLD, 9));
            g.drawImage(statusImage, 0, 0, null);
            g.setComposite(ac);
            g.drawString(new Integer(index+1).toString(), 14, 8);

            img = buffImage;
        }
        else {
            img = statusImage;
        }
        return img;
    }

    /**
     * Updates the chat transport presence status.
     * 
     * @param chatTransport The chat transport to update.
     */
    public void updateTransportStatus(ChatTransport chatTransport)
    {
        JMenuItem menuItem;
        Icon icon;

        if (chatTransport.equals(chatSession.getCurrentChatTransport())
            && !chatTransport.getStatus().isOnline())
        {
            ChatTransport newChatTransport
                = chatSession.getCurrentChatTransport();

            if(newChatTransport.getStatus().isOnline())
                this.setSelected(newChatTransport);
        }

        if (!containsOtherOnlineContacts(chatTransport)
            && chatTransport.getStatus().isOnline())
        {
            this.setSelected(chatTransport);
        }

        menuItem = (JMenuItem) transportMenuItems.get(chatTransport);
        icon = new ImageIcon(createTransportStatusImage(chatTransport));

        menuItem.setIcon(icon);
        if(menu.getSelectedObject().equals(chatTransport))
        {
            this.menu.setIcon(icon);
        }
    }

    /**
     * In the "send via" menu selects the given contact and sets the given icon
     * to the "send via" menu button.
     * 
     * @param protoContact
     * @param icon
     */
    private void setSelected(ChatTransport chatTransport, ImageIcon icon)
    {
        this.chatSession.setCurrentChatTransport(chatTransport);

        SelectedObject selectedObject = new SelectedObject(icon, chatTransport);

        this.menu.setSelected(selectedObject);

        String tooltipText;

        if(!chatTransport.getDisplayName()
                .equals(chatTransport.getName()))
            tooltipText = chatTransport.getDisplayName()
                + " (" + chatTransport.getName() + ")";
        else
            tooltipText = chatTransport.getDisplayName();

        this.menu.setToolTipText(tooltipText);
    }
    
    /**
     * Sets the selected contact to the given proto contact.
     * @param protoContact the proto contact to select
     */
    public void setSelected(ChatTransport chatTransport)
    {
        this.setSelected(chatTransport,
                new ImageIcon(createTransportStatusImage(chatTransport)));
    }
    
    /**
     * Returns the protocol menu.
     * 
     * @return the protocol menu
     */
    public SIPCommMenu getMenu()
    {
        return menu;
    }

    /**
     * Searches online contacts in the send via combo box.
     * 
     * @return TRUE if the send via combo box contains online contacts, otherwise
     * returns FALSE.
     */
    private boolean containsOtherOnlineContacts(ChatTransport chatTransport)
    {
        Enumeration e = transportMenuItems.keys();

        while(e.hasMoreElements())
        {
            ChatTransport comboChatTransport = (ChatTransport) e.nextElement();

            if(!comboChatTransport.equals(chatTransport)
                && comboChatTransport.getStatus().isOnline())
                return true;
        }

        return false;
    }
}
