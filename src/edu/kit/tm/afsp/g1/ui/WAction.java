package edu.kit.tm.afsp.g1.ui;

import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

public abstract class WAction extends AbstractAction {
    private static final long serialVersionUID = 8637960038116053181L;

    public WAction() {
	init();
    }

    protected void init() {

    }

    public void setAsset(String name) {
	URL location = getClass().getResource("assets/" + name + ".png");
	Icon ico = new ImageIcon(location);
	setSmallIcon(ico);
    }

    public void setShortDescription(String desc) {
	putValue(SHORT_DESCRIPTION, desc);
    }

    public void setToolTip(String desc) {
	setShortDescription(desc);
    }

    public void setLongDescription(String desc) {
	putValue(LONG_DESCRIPTION, desc);
    }

    public void setDisplayedMnemonicIndexKey(int index) {
	putValue(DISPLAYED_MNEMONIC_INDEX_KEY, index);
    }

    public void setLargeIcon(Icon ico) {
	putValue(LARGE_ICON_KEY, ico);
    }

    public void setSmallIcon(Icon ico) {
	putValue(SMALL_ICON, ico);
    }

    public void setText(String text) {
	putValue(NAME, text);
    }

    // TODO type?
    public void setSelected(int i) {
	putValue(SELECTED_KEY, i);
    }

    public void setMnemonic(int i) {
	putValue(MNEMONIC_KEY, i);
    }

    public void setAccelerator(KeyStroke stroke) {
	putValue(ACCELERATOR_KEY, stroke);
    }

    public void setName(String string) {
	putValue(DEFAULT, string);
	putValue(NAME, string);
    }
}
