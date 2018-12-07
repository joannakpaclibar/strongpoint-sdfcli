package org.strongpoint.sdfcli.plugin.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.strongpoint.sdfcli.plugin.services.DeployCliService;
import org.strongpoint.sdfcli.plugin.utils.Credentials;

public class DeployDialog extends TitleAreaDialog{
	
	private Text accountIDText;
	
//	private Text emailText;
//	
//	private Text passwordText;
//	
//	private Text sdfcliPath;
	
	private JSONObject results;
	
	private String projectPath;
	
	private IWorkbenchWindow window;
	
	private Shell parentShell;

	public DeployDialog(Shell parentShell) {
		super(parentShell);
		this.parentShell = parentShell;
	}
	
	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}
	
	public void setWorkbenchWindow(IWorkbenchWindow window) {
		this.window = window;
	}		
	
	public JSONObject getResults() {
		return this.results;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Deploy");
		setMessage("Deploy Objects", IMessageProvider.INFORMATION);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);
        
        createAccountIDElement(container);
//        emailElement(container);
//        passwordElement(container);
//        sdfcliPathElement(container);
        
		return area;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(450, 200);
	}
	
	@Override
	protected void okPressed() {
		System.out.println("[Logger] --- Deploy Dialog OK button is pressed");
		JSONObject creds = Credentials.getCredentialsFromFile();
		String emailCred = "";
		String passwordCred = "";
		String pathCred = "";
		String params = "";
		if(creds != null) {
			emailCred = creds.get("email").toString();
			passwordCred = creds.get("password").toString();
			pathCred = creds.get("path").toString();
		}
		String crId = getCurrentProject(window).getName().substring(0, getCurrentProject(window).getName().indexOf("_"));
		if(crId != null && !crId.isEmpty()) {
			params = crId;
		} else {
			params = String.join(",",getScripIds(window));
			System.out.println("DEPLOY SCRIPT IDS: " +params);
		}
		JSONObject approveResult = DeployCliService.newInstance().isApprovedDeployment(parentShell, accountIDText.getText(), emailCred, passwordCred, params);
		System.out.println("Deploy Dialog results: " +approveResult.toJSONString());
		JSONObject data = (JSONObject) approveResult.get("data");
		JSONObject supportedObjs = DeployCliService.newInstance().getSupportedObjects(accountIDText.getText(), emailCred, passwordCred);
		if(hasUnsupportedObjects(getScripIds(this.window), supportedObjs)) {
			MessageDialog.openWarning(this.parentShell, "Unsupported Objects Detected", "Please manually complete and validate using Environment Compare.");
		} else {
			if(!(boolean)data.get("result")) {
				JSONObject messageObject = new JSONObject();
				messageObject.put("message", approveResult.get("message").toString());
				results = messageObject;
			} else {
				results = DeployCliService.newInstance().deployCliResult(accountIDText.getText(), emailCred, passwordCred, pathCred, this.projectPath);	
			}			
		}	
		super.okPressed();
	}
	
	private void createAccountIDElement(Composite container) {
        Label accountIDLabel = new Label(container, SWT.NONE);
        accountIDLabel.setText("Account ID: ");

        GridData accountIDGridData = new GridData();
        accountIDGridData.grabExcessHorizontalSpace = true;
        accountIDGridData.horizontalAlignment = GridData.FILL;

        accountIDText = new Text(container, SWT.BORDER);
        accountIDText.setLayoutData(accountIDGridData);
	}
	
	public static IProject getCurrentProject(IWorkbenchWindow window) {
		ISelectionService selectionService = window.getSelectionService();
		ISelection selection = selectionService.getSelection();
		IProject project = null;
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			if (element instanceof IResource) {
				project = ((IResource) element).getProject();
			} else if (element instanceof PackageFragmentRoot) {
				IJavaProject jProject = ((PackageFragmentRoot) element).getJavaProject();
				project = jProject.getProject();
			} else if (element instanceof IJavaElement) {
				IJavaProject jProject = ((IJavaElement) element).getJavaProject();
				project = jProject.getProject();
			}
		}
		return project;
	}	
	
    public List<String> getScripIds(IWorkbenchWindow window){
    	List<String> scriptIds = new ArrayList<String>();
        ISelectionService selectionService = window.getSelectionService();    
        ISelection selection = selectionService.getSelection();    
        IProject project = null;    
        if(selection instanceof IStructuredSelection) {    
            Object element = ((IStructuredSelection)selection).getFirstElement();    
            if (element instanceof IResource) {    
                project= ((IResource)element).getProject();    
            } else if (element instanceof PackageFragmentRoot) {    
                IJavaProject jProject = ((PackageFragmentRoot)element).getJavaProject();    
                project = jProject.getProject();    
            } else if (element instanceof IJavaElement) {    
                IJavaProject jProject= ((IJavaElement)element).getJavaProject();    
                project = jProject.getProject();    
            }    
        } 
        IPath path = project.getRawLocation();
        IContainer container = project.getWorkspace().getRoot().getContainerForLocation(path);
        try {
			IContainer con = (IContainer) container.findMember("Objects");
			for (IResource res : con.members()) {
				if (res.getFileExtension().equalsIgnoreCase("xml")) {
					String id = res.getName().substring(0, res.getName().indexOf("."));
					scriptIds.add(id);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
        
        return scriptIds;    
    } 
    
    private boolean hasUnsupportedObjects(List<String> scriptIds, JSONObject supportedObjects) {
    	boolean hasUnsupportedObj = false;
    	JSONArray data = (JSONArray) supportedObjects.get("data");
    	List<String> supportedList = new ArrayList<String>();
		if (data != null) { 
			int size = data.size();
			for (int i = 0 ; i < size ; i++){ 
				supportedList.add(data.get(i).toString());
			} 
		}
		for (String scriptId : scriptIds) {
			if(!supportedList.contains(scriptId)) {
				hasUnsupportedObj = true;
				break;
			}
		}
    	
    	return hasUnsupportedObj;
    }
	
//	private void emailElement(Composite container) {
//        Label emailLabel = new Label(container, SWT.NONE);
//        emailLabel.setText("Email: ");
//
//        GridData emailGridData = new GridData();
//        emailGridData.grabExcessHorizontalSpace = true;
//        emailGridData.horizontalAlignment = GridData.FILL;
//
//        emailText = new Text(container, SWT.BORDER);
//        emailText.setLayoutData(emailGridData);
//	}
//	
//	private void passwordElement(Composite container) {
//        Label passwordLabel = new Label(container, SWT.NONE);
//        passwordLabel.setText("Password: ");
//
//        GridData passwordGridData = new GridData();
//        passwordGridData.grabExcessHorizontalSpace = true;
//        passwordGridData.horizontalAlignment = GridData.FILL;
//
//        passwordText = new Text(container, SWT.BORDER);
//        passwordText.setLayoutData(passwordGridData);
//        passwordText.setEchoChar('*');
//	}
//	
//	private void sdfcliPathElement(Composite container) {
//        Label sdfcliPathLabel = new Label(container, SWT.NONE);
//        sdfcliPathLabel.setText("SDFCLI Path: ");
//
//        GridData sdfcliPathGridData = new GridData();
//        sdfcliPathGridData.grabExcessHorizontalSpace = true;
//        sdfcliPathGridData.horizontalAlignment = GridData.FILL;
//
//        sdfcliPath = new Text(container, SWT.BORDER);
//        sdfcliPath.setLayoutData(sdfcliPathGridData);
//        sdfcliPath.setToolTipText("Path to your SDFCLI executable(i.e /path/to/sdfcli/)");
//        
//	}	


}
