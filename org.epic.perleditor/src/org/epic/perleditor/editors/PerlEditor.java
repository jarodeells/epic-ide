package org.epic.perleditor.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.epic.perleditor.views.PerlOutlinePage;
import org.epic.perleditor.views.model.Model;
import org.epic.perleditor.views.model.Module;
import org.epic.perleditor.views.model.Subroutine;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.editors.util.PerlColorProvider;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

import cbg.editor.*;

/**
 * Perl specific text editor.
 */

public class PerlEditor
	extends TextEditor
	implements ISelectionChangedListener, IPropertyChangeListener {

	//implements ISelectionChangedListener {

	/** The outline page */
	private PerlContentOutlinePage fOutlinePage;
	protected PerlOutlinePage page;
	protected PerlSyntaxValidationThread fValidationThread = null;
	protected CompositeRuler ruler;
	protected LineNumberRulerColumn numberRuler;
	private boolean lineRulerActive = false;
	private SourceViewer fSourceViewer;
	private IDocumentProvider fDocumentProvider;

	/**
	 * Default constructor();
	 */

	public PerlEditor() {
		super();
		setDocumentProvider(new ColoringDocumentProvider());

		PerlEditorPlugin
			.getDefault()
			.getPreferenceStore()
			.addPropertyChangeListener(
			this);

		this.setPreferenceStore(
			PerlEditorPlugin.getDefault().getPreferenceStore());
	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method extend the 
	 * actions to add those specific to the receiver
	 */

	protected void createActions() {
		super.createActions();

		IDocumentProvider provider = getDocumentProvider();
		IDocument document = provider.getDocument(getEditorInput());
		getSourceViewer().setDocument(document);

		fDocumentProvider = provider;
		fSourceViewer = (SourceViewer) getSourceViewer();

		if (fValidationThread == null) {
			fValidationThread =
				new PerlSyntaxValidationThread(this, getSourceViewer());
			//Thread defaults
			fValidationThread.start();
		}
		fValidationThread.setText(getSourceViewer().getTextWidget().getText());

		setEditorForegroundColor();

	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs any extra 
	 * disposal actions required by the Perl editor.
	 */

	public void dispose() {
		try {
			IEditorInput input = this.getEditorInput();
			IResource resource =
				(IResource) ((IAdaptable) input).getAdapter(IResource.class);

			resource.deleteMarkers(IMarker.PROBLEM, true, 1);
			fValidationThread.dispose();
			super.dispose();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs any extra 
	 * revert behavior required by the Perl editor.
	 */

	public void doRevertToSaved() {

		super.doRevertToSaved();

	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs any extra 
	 * save behavior required by the Perl editor.
	 */

	public void doSave(IProgressMonitor monitor) {

		super.doSave(monitor);

		if (page != null) {
			page.update();
		}

		if (fValidationThread != null) {
			fValidationThread.setText(
				getSourceViewer().getTextWidget().getText());
		}

	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs any extra 
	 * save as behavior required by the Perl editor.
	 */

	public void doSaveAs() {

		super.doSaveAs();

		if (page != null) {
			page.update();
		}

		if (fValidationThread != null) {
			fValidationThread.setText(
				getSourceViewer().getTextWidget().getText());
		}

	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs sets the 
	 * input of the outline page after AbstractTextEditor has set input.
	 */

	public void doSetInput(IEditorInput input) throws CoreException {

		super.doSetInput(input);

	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method adds any 
	 * PerlEditor specific entries.
	 */

	public void editorContextMenuAboutToShow(MenuManager menu) {

		super.editorContextMenuAboutToShow(menu);

	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs gets
	 * the Perl content outline page if request is for a an 
	 * outline page.
	 */

	public Object getAdapter(Class required) {
		if (required.equals(IContentOutlinePage.class)) {
			IEditorInput input = getEditorInput();

			if (input instanceof IFileEditorInput) {
				page = new PerlOutlinePage(getSourceViewer());
				page.addSelectionChangedListener(this);
				return page;
			}

		}

		return super.getAdapter(required);
	}

	public void updateOutline() {
		if (page != null) {
			page.update();
		}

	}

	/* 
	 * Method declared on AbstractTextEditor
	 */
	// TODO
	protected void initializeEditor() {
		//TODO ?????
		//PerlEditorEnvironment.connect(this);
		setSourceViewerConfiguration(
			new PerlSourceViewerConfiguration(
				PerlEditorPlugin.getDefault().getPreferenceStore(),
				this));
		//setRulerContextMenuId("#TextRulerContext");
		super.initializeEditor();
	}

	/* TEST */

	protected final ISourceViewer createSourceViewer(
		Composite parent,
		IVerticalRuler ruler,
		int styles) {
		ISharedTextColors sharedColors =
			EditorsPlugin.getDefault().getSharedTextColors();
		fOverviewRuler =
			new OverviewRuler(
				fAnnotationAccess,
				VERTICAL_RULER_WIDTH,
				sharedColors);
		//ISourceViewer viewer = super.createSourceViewer(parent, ruler, styles);
		SourceViewer viewer =
			new PerlSourceViewer(parent, ruler, fOverviewRuler, true, styles);
		viewer.showAnnotations(true);
		viewer.showAnnotationsOverview(true);

		return viewer;
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 * @since 2.1
	 */
	/*
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
	
		fAnnotationAccess= createAnnotationAccess();
		ISharedTextColors sharedColors= EditorsPlugin.getDefault().getSharedTextColors();
	
		fOverviewRuler= new OverviewRuler(fAnnotationAccess, VERTICAL_RULER_WIDTH, sharedColors);
	
		Iterator e= fAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			AnnotationPreference preference= (AnnotationPreference) e.next();
			if (preference.contributesToHeader())
				fOverviewRuler.addHeaderAnnotationType(preference.getAnnotationType());
		}
	
		ISourceViewer sourceViewer= new PerlSourceViewer(parent, ruler, fOverviewRuler, isOverviewRulerVisible(), styles);
		fSourceViewerDecorationSupport= new SourceViewerDecorationSupport(sourceViewer, fOverviewRuler, fAnnotationAccess, sharedColors);
		configureSourceViewerDecorationSupport();
	
		return sourceViewer;
	}
	*/
	/*
		protected IVerticalRuler createVerticalRuler() {
			ruler = new CompositeRuler();
			ruler.addDecorator(0, new AnnotationRulerColumn(16));
	
			numberRuler = new LineNumberRulerColumn();
			numberRuler.setBackground(
				new Color(Display.getCurrent(), 230, 230, 230));
			numberRuler.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
	
			if (PerlEditorPlugin.getDefault().getShowLineNumbersPreference()) {
				ruler.addDecorator(1, numberRuler);
				lineRulerActive = true;
			}
	
			return ruler;
		}
		*/

	/**
	* @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	*/

	public void selectionChanged(SelectionChangedEvent event) {
		if (event != null) {
			if (event.getSelection() instanceof IStructuredSelection) {
				IStructuredSelection sel =
					(IStructuredSelection) event.getSelection();
				if (sel != null) {
					if (sel.getFirstElement() instanceof Module
						|| sel.getFirstElement() instanceof Subroutine) {
						Model fe = (Model) sel.getFirstElement();
						if (fe != null) {
							selectAndReveal(fe.getStart(), fe.getLength());
						}
					}
				}
			}
		}
	}

	public void revalidateSyntax(boolean forceUpdate) {

		if (fValidationThread != null) {
			fValidationThread.setText(
				getSourceViewer().getTextWidget().getText(),
				forceUpdate);
		}

	}

	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		revalidateSyntax(false);

		if (page != null) {
			page.update();
		}
	}

	/*
		 * @see IPropertyChangeListener.propertyChange()
		 */

	public void propertyChange(PropertyChangeEvent event) {

		if (event.getProperty().equals("PERL_EXECUTABLE")) {
			return;
		}

		PerlSourceViewerConfiguration viewerConfiguration =
			(PerlSourceViewerConfiguration) this.getSourceViewerConfiguration();
		viewerConfiguration.adaptToPreferenceChange(event);

		IAnnotationModel model = fSourceViewer.getAnnotationModel();
		IDocument document = fDocumentProvider.getDocument(getEditorInput());
		fSourceViewer.refresh();
		fSourceViewer.setDocument(document, model);

		setEditorForegroundColor();

	}

	public ISourceViewer getViewer() {
		return getSourceViewer();
	}

	private void setEditorForegroundColor() {
		// Set text editor forground colour
		RGB rgb =
			PreferenceConverter.getColor(
				PerlEditorPlugin.getDefault().getPreferenceStore(),
				PreferenceConstants.EDITOR_STRING_COLOR);
		getSourceViewer().getTextWidget().setForeground(
			PerlColorProvider.getColor(rgb));
	}
}