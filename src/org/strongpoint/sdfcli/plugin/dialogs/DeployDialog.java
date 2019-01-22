package org.strongpoint.sdfcli.plugin.dialogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.strongpoint.sdfcli.plugin.services.DeployCliService;
import org.strongpoint.sdfcli.plugin.utils.Accounts;
import org.strongpoint.sdfcli.plugin.utils.Credentials;

public class DeployDialog extends TitleAreaDialog{
	
	private Combo accountIDText;
	
	private JSONObject results;
	
	private String projectPath;
	
	private IWorkbenchWindow window;
	
	private Shell parentShell;
	
	private String selectedValue = "";

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
        
		return area;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(450, 210);
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
		} else {
			if(!Credentials.isCredentialsFileExists()) {
				MessageDialog.openError(this.parentShell, "No user credentials found", "Please set user credentials in Strongpoint > Credentials Settings menu");
			}
		}
		String crId = getCurrentProject(window).getName().substring(0, getCurrentProject(window).getName().indexOf("_"));
		if(crId != null && !crId.isEmpty()) {
			params = crId;
		} else {
			params = String.join(",",getScripIds(window));
			System.out.println("DEPLOY SCRIPT IDS: " +params);
		}
		String accountId = selectedValue.substring(selectedValue.indexOf("(") + 1, selectedValue.indexOf(")"));
		JSONObject approveResult = DeployCliService.newInstance().isApprovedDeployment(parentShell, accountId, emailCred, passwordCred, String.join(",",getScripIds(window)));
		System.out.println("Deploy Approve results: " +approveResult.toJSONString());
		JSONObject data = (JSONObject) approveResult.get("data");
		JSONObject supportedObjs = DeployCliService.newInstance().getSupportedObjects(accountId, emailCred, passwordCred);
		JSONObject importObjects = readImportJsonFile(this.projectPath);
		JSONArray objects = (JSONArray) importObjects.get("objects");
		List<String> listStr = new ArrayList<String>(); 
		boolean isExcessObj = false;
		if(objects != null) { 
			for (int i = 0; i < objects.size(); i++){ 
				listStr.add(objects.get(i).toString());
			} 
		} 		
		for (String objStr : getScripIds(this.window)) {
			if(!listStr.contains(objStr)) {
				isExcessObj = true;
				break;
			}
		}
		if(isExcessObj) {
			MessageDialog.openError(this.parentShell, "Object List Error", "Objects in the project changed after approval. Request a new Change Request.");
		} else {
			if(hasUnsupportedObjects(getScripIds(this.window), supportedObjs)) {
				MessageDialog.openWarning(this.parentShell, "Unsupported Objects Detected", "Please manually complete and validate using Environment Compare.");
			} else {
//				JSONObject policyObj = DeployCliService.newInstance().getPolicy();
//				if(policyObj != null && (boolean)policyObj.get("results")) {
//					if(!(boolean)data.get("result")) {
//						JSONObject messageObject = new JSONObject();
//						messageObject.put("message", approveResult.get("message").toString());
//						results = messageObject;
//					}  else {
//						results = DeployCliService.newInstance().deployCliResult(accountId, emailCred, passwordCred, pathCred, this.projectPath);	
//					}				
//				} else {
//					results = DeployCliService.newInstance().deployCliResult(accountId, emailCred, passwordCred, pathCred, this.projectPath);
//				}
				if(!(boolean)data.get("result")) {
					JSONObject messageObject = new JSONObject();
					messageObject.put("message", approveResult.get("message").toString());
					results = messageObject;
				} else {
					results = DeployCliService.newInstance().deployCliResult(accountId, emailCred, passwordCred, pathCred, this.projectPath);	
				}			
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

        accountIDText = new Combo(container, SWT.BORDER);
        accountIDText.setItems(Accounts.getAccountsStrFromFile());
        accountIDText.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				selectedValue = accountIDText.getText();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});        
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
				System.out.println("SUPPORTED OBJECT: " +data.get(i).toString());
				supportedList.add(data.get(i).toString());
			} 
		}
		for (String scriptId : scriptIds) {
			System.out.println("SCRIPT ID: " +scriptId);
			for(String supportedObj : supportedList) {
				if(supportedObj.contains(scriptId)) {
					hasUnsupportedObj = true;
					break;
				}				
			}
		}
    	
    	return hasUnsupportedObj;
    }
    
	private JSONObject readImportJsonFile(String projectPath) {
		StringBuilder contents = new StringBuilder();
		String str;
		File file = new File(projectPath + "/import.json");
		System.out.println("SYNC PROJECT PATH: " + projectPath + "/import.json");
		JSONObject scriptObjects = null;
		try {
			if(file.exists() && !file.isDirectory()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				while((str = reader.readLine())  != null) {
					contents.append(str);
				}
				System.out.println("FILE Contents: " +contents.toString());
				scriptObjects = (JSONObject) new JSONParser().parse(contents.toString());	
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return scriptObjects;		
	}      

}
