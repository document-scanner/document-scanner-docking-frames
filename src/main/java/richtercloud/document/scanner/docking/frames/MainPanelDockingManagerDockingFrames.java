/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.document.scanner.docking.frames;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.MultipleCDockable;
import bibliothek.gui.dock.common.event.CVetoFocusListener;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.util.DockUtilities;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.document.scanner.ifaces.Constants;
import richtercloud.document.scanner.ifaces.EntityPanel;
import richtercloud.document.scanner.ifaces.MainPanel;
import richtercloud.document.scanner.ifaces.MainPanelDockingManager;
import richtercloud.document.scanner.ifaces.OCRPanel;
import richtercloud.document.scanner.ifaces.OCRSelectComponent;

/**
 *
 * @author richter
 */
public class MainPanelDockingManagerDockingFrames implements MainPanelDockingManager {
    static {
        /*
        doesn't prevent `java.lang.IllegalStateException: During an operation the framework attempted to acquire the same lock twice. There are two possible explanations:
        1. In a multi-threaded application one or both operations are not executed in the EventDispatchThread, or
        2. The operations are calling each other, which should not happen.
        Please verify that this application is not accessing the framework from different threads, and fill a bugreport if you feel that this exception is not caused by your application.`
        as suggested in the error message
        */
        DockUtilities.disableCheckLayoutLocked();
    }
    private CControl control;
    private final Map<JComponent, MultipleCDockable> dockableMap = new HashMap<>();
    private final Map<CDockable, OCRSelectComponent> componentMap = new HashMap<>();
    private MainPanel mainPanel;

    public MainPanelDockingManagerDockingFrames() {
    }

    @Override
    public void init(JFrame dockingControlFrame,
            MainPanel mainPanel) {
        this.control = new CControl (dockingControlFrame);
        this.mainPanel = mainPanel;
        mainPanel.add(this.control.getContentArea(),
                BorderLayout.CENTER); //has to be called after initComponents
        this.control.addVetoFocusListener(new CVetoFocusListener() {
            @Override
            public boolean willGainFocus(CDockable dockable) {
                OCRSelectComponent aNew = componentMap.get(dockable);
                if(aNew != null
                        && !aNew.equals(mainPanel.getoCRSelectComponent())
                        //focused component requests focus (e.g. after newly
                        //adding)
                ) {
                    switchDocument(mainPanel.getoCRSelectComponent(),
                            aNew);
                    mainPanel.setoCRSelectComponent(aNew);
                }
                return true;
            }

            @Override
            public boolean willLoseFocus(CDockable dockable) {
                return true;
            }
        });
    }

    /**
     * A method which is called after a new {@link OCRSelectComponent}
     * has been created in {@link #addDocument(java.util.List, java.io.File) }
     * which adds the created component to the docking framework which triggers
     * the {@link CVetoFocusListener} added to {@code control}.
     * @param old
     * @param aNew
     */
    @Override
    public void addDocumentDockable(OCRSelectComponent old,
            OCRSelectComponent aNew) {
        MultipleCDockable aNewDockable = dockableMap.get(aNew);
        if(aNewDockable == null) {
            aNewDockable = new DefaultMultipleCDockable(null,
                    aNew.getoCRSelectPanelPanel().getDocumentFile() != null
                            ? aNew.getoCRSelectPanelPanel().getDocumentFile().getName()
                            : Constants.UNSAVED_NAME,
                    aNew);
            dockableMap.put(aNew, aNewDockable);
            componentMap.put(aNewDockable, aNew);
        }
        control.addDockable(aNewDockable);
        MultipleCDockable oldDockable = dockableMap.get(old);
        assert oldDockable != null;
        if(old != null) {
            aNewDockable.setLocationsAside(oldDockable);
        }else {
            aNewDockable.setLocation(CLocation.base().normalNorth(0.4));
        }
        aNewDockable.setVisible(true);
        if(old == null) {
            //first document added -> CVetoFocusListener methods not
            //triggered
            switchDocument(old,
                    aNew);
        }
    }

    /**
     * Handles both switching documents (if {@code old} and {@code aNew} are not
     * {@code null} and adding the first document (if {@code old} is
     * {@code null}.
     * @param old
     * @param aNew
     */
    /*
    internal implementation notes:
    - handling both switching and adding the first document maximizes code
    reusage
    */
    @Override
    public void switchDocument(OCRSelectComponent old,
            final OCRSelectComponent aNew) {
        synchronized(aNew.getTreeLock()) {
            Pair<OCRPanel, EntityPanel> newPair = mainPanel.getDocumentSwitchingMap().get(aNew);
            assert newPair != null;
            OCRPanel oCRPanelNew = newPair.getKey();
            EntityPanel entityPanelNew = newPair.getValue();
            assert oCRPanelNew != null;
            assert entityPanelNew != null;
            //check if dockables already exist in order to avoid failure of
            //CControl.replace if dockable is recreated
            MultipleCDockable oCRPanelNewDockable = dockableMap.get(oCRPanelNew);
            if(oCRPanelNewDockable == null) {
                oCRPanelNewDockable = new DefaultMultipleCDockable(null,
                        "OCR result",
                        oCRPanelNew);
                dockableMap.put(oCRPanelNew, oCRPanelNewDockable);
            }
            final MultipleCDockable oCRPanelNewDockable0 = oCRPanelNewDockable;
            MultipleCDockable entityPanelNewDockable = dockableMap.get(entityPanelNew);
            if(entityPanelNewDockable == null) {
                entityPanelNewDockable = new DefaultMultipleCDockable(null,
                        "Entities",
                        entityPanelNew);
                dockableMap.put(entityPanelNew, entityPanelNewDockable);
            }
            final MultipleCDockable entityPanelNewDockable0 = entityPanelNewDockable;
            if(old != null) {
                Pair<OCRPanel, EntityPanel> oldPair = mainPanel.getDocumentSwitchingMap().get(old);
                assert oldPair != null;
                //order doesn't matter
                OCRPanel oCRPanelOld = oldPair.getKey();
                EntityPanel entityPanelOld = oldPair.getValue();
                assert oCRPanelOld != null;
                assert entityPanelOld != null;
                final MultipleCDockable oCRPanelOldDockable = dockableMap.get(oCRPanelOld);
                final MultipleCDockable entityPanelOldDockable = dockableMap.get(entityPanelOld);
//                SwingUtilities.invokeLater(new Runnable() {
//                    @Override
//                    public void run() {
                control.replace(oCRPanelOldDockable, oCRPanelNewDockable0);
                //CControl.replace fails if new dockable is already
                //registered at CControl
                //CControl.replace fails if old dockable has already been
                //removed from CControl
                //CDockable.setVisible(false) unregisters dockable at
                //CControl
                oCRPanelNewDockable0.setVisible(true);
                control.replace(entityPanelOldDockable, entityPanelNewDockable0);
                //MultipleCDockable.setVisible(true) fails if it's not
                //registered at a CControl (which has to be done with
                //CControl.replace (see above))
                entityPanelNewDockable0.setVisible(true);
//                    }
//                });//why SwingUtilities.invokeLater?
            }else {
                //order matters
                control.addDockable(oCRPanelNewDockable);
                control.addDockable(entityPanelNewDockable);
                oCRPanelNewDockable.setLocation(CLocation.base().normalEast(0.4));
                oCRPanelNewDockable.setVisible(true);
                entityPanelNewDockable.setLocation(CLocation.base().normalSouth(0.25));
                entityPanelNewDockable.setVisible(true);
            }
            mainPanel.setoCRSelectComponent(aNew);
            mainPanel.validate(); //@TODO necessary
        }
    };

    @Override
    public void removeDocument(OCRSelectComponent oCRSelectComponent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
