//def fullApi = "certified-extension-package-test-${pluginVersion}-mule-plugin"
//def extensionName = "certified-extension-package-test-${pluginVersion}-mule-plugin"

def exchangeTmpFolder = new File(basedir, '.exchange_modules_tmp')
def targetFolder = new File(exchangeTmpFolder, 'target')
def fullApiFolder =new File(targetFolder, 'full_api MDSLKJDASKLJKLASDKL')
assert fullApiFolder.exists() : ' TIENE QUE REVENTAR ACA PERO SIGUE DE LARGO.. folder must exist'

//def extensionName = "certified-extension-package-test-${pluginVersion}-mule-plugin"
//def file = new File(basedir, 'target/' + extensionName + '.jar');
//assert file.exists() : 'Zip file must exist'