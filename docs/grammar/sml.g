grammar sml;

/* <PROVIDER-RULES> */
srvSignature : prvSignature | opSignature | bldrSignature | 'sig' '(' srvSignature ',' signatureOp ')' ;

prvSignature : 'sig' '(' ( sigName ',' )? prvSpec ')' ;

opSignature  : 'sig' '(' (( sigName ',' )? opSpec ',' prvSpec ')' | prvSignature ',' selector ')' | prvSignature ',' opSpec) ')' ;

ntlSignature : 'sig' '(' ( sigName ',' )? 'filePath' '(' (netletFilename | netletArtifact) ')' ')' ;

bldrSignature : 'sig' '('( sigName ',' )? classSelector ','classType ')' ;

prvSpec : (srvType | 'fiType' '(' srvTypeName ')') (',' matchTypes)? (',' (prvId))? (',' prvDeployment)? | bldrSignature | prvInstance ;
srvType : classType | interfaceType ;
opSpec  : (selector | signatureOp) (',' srvResult)? (',' inputConnector)? (',' outputConnector)? ( ',' dataContext )? ;
matchTypes : 'types' '(' (interfaceType ',')* interfaceType ')' ; 
inputConnector : 'inConn' '(' (mapEntry ',')* mapEntry ')' ;
outputConnector : 'outConn' '(' (mapEntry ',')* mapEntry ')' ;

signatureOp : 'op' '(' (selector (',' opArg)* ')' |opSignature) ')' ;
opArg : accessType | flowType | provisionable | monitorable | waitable | fiManagement | srvShellExec;
accessType : 'Access.PUSH' | 'Access.PULL' ;
flowType: 'Flow.PAR' | 'Flow.SEQ' ;
provisionable : 'Provision.YES' | 'Provision.NO' ;
monitorable : 'Monitor.YES' | 'Monitor.NO' ;
waitable : 'Wait.YES' | 'Wait.NO' ;
fiManagement : 'FidelityManagement.YES' | 'FidelityManagement.NO' ;
srvShellExec : 'Shell.LOCAL' | 'Shell.REMOTE' ;
prvId : 'srvName' '(' serviceName (',' 'locators' '('(locatorName',')+ ')')? ((',' groupName)+)? ')' | 'prvName' '(' providerName ')' ;
srvResult : 'result' '(' pathName? (',' inputPaths)? (',' outputPaths)? (',' dataContext)? ')' ;
prvDeployment : 'deploy' '(' ('configuration' '('configName ')' | prvCodeSpec) deployOptions ')' ;	
	
prvCodeSpec : 'implementation' '(' prvClassName ')' ','prvClasspath ',' prvCodebase ;		   
prvClasspath : 'classpath' '(' (jarName',')* jarName')';
prvCodebase : 'codebase' '(' (jarName',')* jarName')';
deployOptions :	(',''maintain' '(' intNumber ')')? (',''perNode' '(' intNumber ')')? (',''idle' '(' intNumber ')')? ;

inputPaths : 'inPaths' '(' (srvPath',')* srvPath')' ;
outputPaths : 'outPaths' '(' (srvPath',')* srvPath')' ;


/* <MULTIFIDELITY-RULES> */

multiFi : entFidelity | sigFidelity | morphFidelity | reqFidelity | varFidelity | fiMogram ;

entFidelity : 'eFi' '(' (contextEntry',')* contextEntry ')' ;

sigFidelity : 'sFi' '(' fiName ',' (opSignature',')* opSignature ')' ;

morphFidelity : 'mFi' '(' fiName ',' srvMorpher? (srvRequest',')+ srvRequest')' ;
			
reqFidelity : 'rFi' '('(fiName',')? (srvRequest',')+ srvRequest ')' ; 

varFidelity : 'vFi' '(' fiName (',' (value | opSignature | contextEntry)
			| (',' srvRoutine)? ( ',' entGetter)? ( ',' entSetter)?) ')' ;
			
srvMorpher: morpherLambdaExpression ;


/* <PROVIDER-SERVICES> */

prvInstance : 'prv' '(' srvSignature ')' ;


/* <REQUESTS> */

srvRequest : srvSignature | contextEntry | multiFi | srvMogram ;


/* <ENTRIES> */

annotatedPath : 'path' '(' pathName ( ',' pathTag)? ')' ;
mapPath : 'map' '(' toPath ',' fromPath ')' ;
srvPath : pathName | annotatedPath |mapPath ;
entOp : 'inVal' | 'outVal' | 'inoutVal' | 'dbVal' ;
dataEntry : entOp '(' srvPath ',' value ')' ;

contextEntry : dataEntry | procEntry | srvEntry | varEntry | fiEntry
			| entType '(' (pathName ',')? contextEntry ')' ;
			
procEntry : ('ent' '(' pathName ',' (srvEvaluator | srvInvoker ( ',' entModel)?) ')' | sigEntry | lambdaEntry) ;

srvRoutine : contextEntry | srvInvoker | srvEvaluator ;

sigEntry : 'ent' '(' (pathName ',')? opSignature ')' ;

mapEntry : 'ent' '(' fromPathName ',' toPathName ')' ;

lambdaEntry : 'lambda' '(' pathName ',' (entrycallableLambdaExpression
			| serviceLambdaExpression
			| callableLambdaExpression
			| clientLambdaExpression
			| valueCallableLambdaExpression)(',' srvArgs)? ')' ;

srvEntry : 'ent' '(' pathName ',' (opSignature  (',' entModel)? (',' cxtSelector)?  
			| srvRoutine | srvMogram) ')' ;

cxtSelector : selector '(' (componentName)? (',' pathName)+ ')' ;

varEntry : ('var' '(' (pathName ( ',' (value | opSignature | morphFidelity 
			| srvRoutine 
			| varProxy 
			| contextEntry )
			|(',' varFidelity)+ ) ')')
			| entVar | objectiveVar | constraintVar) ;

fiEntry : 'ent' '(' pathName (entFidelity ',')* entFidelity ')' ;

entType : 'in' | 'out' | 'inout' | 'db' ;

varProxy : 'proxy' '(' pathName ',' opSignature ')' ;
		
srvInvoker : 'invoker' '(' (javaExpression ',' 'srvArgs' ( ',' dataContext)? 
			| opSignature) ')' | srvExertion | 'inc' '(' srvInvoker ',' double | int ')'
			| 'methodInvoker(TODO)'
			| 'cmdInvoker(TODO)'
			| 'invoker' '('(name',')? valueCallableLambdaExpression ( ',' contextModel)?',' srvArgs ')'
			| procEntry  | conditionalInvoker ;
		
conditionalInvoker :  'loop' '(' srvCondition ',' srvInvoker ')' 
			| 'loop' '(' min ',' max ',' (srvCondition',')? srvInvoker ')' | 'alt' '(' invokeOption* | invokeOption  ')' ;
			
invokeOption : 'opt' '(' srvCondition ',' srvInvoker ')' ;

srvCondition : 'condition' '(' conditionCallableLambda ')'
		    |  'condition' '(' conditionExpression ','  parameterName* ')' ;

srvArgs : 'args' '(' (argName ',')* argName ')' ;

dependentVars : 'vars' '(' (dependentVarName ',')* dependentVarName ')' ;
srvEvaluator : lambdaEvaluator | entEvaluator | objectImplementingEvaluation ;
entGetter : objectImplementingGetter ;
entSetter : objectImplementingSetter ;

/* <MOGRAMS> */

contextModelType : 'procModel' | 'srvModel' | 'varModel' | 'model' | 'mdl' ;
srvExertionType : 'task' | 'block' | 'job' | conditionalExertion | 'exertion' | 'xrt' ;
conditionalExertionType : 'loop' | 'alt' | 'opt' ;
srvMogramType : contextModelType | srvExertionType | 'mogram' | 'mog' ;

srvMogram : dataContext  | contextModel | srvExertion | fiMogram | 'mogram' '(' (contextModelParameters | srvExertionParamters) ')' ;

fiMogram : 'fiMog' '(' (name',')? (morphFidelity | reqFidelity) dataContext?')' ;


/* <MODELS> */

entModel : dataContext | contextModel | structuredVarModel | contextSnapshotResult ;

dataContext : 'context ' '(' (name',')? (dataEntry',')+ (srvResult)? (',' inputPaths)? (',' outputPaths)? ')' 
			| 'tag' '(' dataContext',' annotatedPath ')' | 'tagAssociation' '(' dataContext',' newTagAssociation ')' ;

contextModel : contextModelType '('(name',' )? (contextEntry',')+ (',' 'response' '('(pathName',')* pathName')')? (',' srvDependency)? ')';
		
parTypes : 'types' '('(srvType',')* srvType')' ;
parArgs : 'args' '('(object',')* object ')' ;
srvDependency : 'dependsOn' '(' ('ent' '('pathName',' 'paths' '('(pathName',')* pathName ')')+ ')' ;

/* <TASKS> */
srvTask : 'task' '('(name',')? ((opSignature',')* opSignature | (sigFidelity',')* sigFidelity | morphFidelity) ',' dataContext')' ;


/* <EXERTIONS> */

srvExertion : srvTask | compoundExertion | 'exertion' '(' srvExertionParamters ')' ;

compoundExertion : srvJob | srvBlock | conditionalExertion ;

srvJob : 'job' '('(name',')? (opSignature | sigFidelity) ',' dataContext (',' srvMogram)+ jobOptions? ')' ;
				
jobOptions : (','contextPipe)* (',' exertionStrategy)? (',' dependency)? (','metaFiSelector)* ;			

srvBlock :	 'block' '('(name',')? (opSignature | sigFidelity)',' (dataContext',')? 
			(srvMogram',')+ (metaFiSelector',')* metaFiSelector ')' ;

conditionalExertion : 'loop' '('srvCondition',' srvMogram')' 
			| 'loop' '('min',' max',' (srvCondition',')? srvMogram')' | 'alt' '('(srvOption',')* srvOption ')' ;

srvOption : 'opt' '('srvCondition',' srvMogram')' ;

contextPipe : 'pipe' '(' 'outPoint' '('srvExertion',' contextPathName')' ',' 
			'inPoint' '('srvExertion',' contextPathName')' ')' ;

exertionStrategy : 'strategy' '(' (accessType',')? (flowType',')? (monitorable',')? (provisionable)? ')' ;

fiSelector : 'fi' '('pathName',' fiName')' ;

metaFiSelector : 'fi''('fiName',' (fiSelector',')* fiSelector ')';

fiList : 'fis''(' (((fiSelector | fiList))',')* (fiSelector | fiList)')' ;

/* <VAR-ORIENTED-MODELING> */

structuredVarModel : responseModeling | parametricModeling | optimizationModeling | streamingParametricModeling ;

responseModeling : 'responseModel' '('(modelName',' )? 
					(modelingInstance',' )?  (basicVars',')+ basicVars ',' varRealizations')' ;

parametricModeling : 'paramericModel' '('(modelName',' )? 
					(modelingInstance',' )?  (basicVars',')+ basicVars ',' varRealizations ','mdlTable ')' ;

varRealizations : ((varRealization',')* (varRealization))? ;

mdlTable : 'table' '('mdlParametricTable',' mdlResponseTable')' ;

streamingParametricModeling : 'streamingParametricModel' '('(modelName',')?	parametricModeling')' ;

optimizationModeling : 'optimizationModel' '(' (modelName',' )? basicVars* ','
					mdlObjectiveVars ','  mdlConstraintVars ',' varRealizations ')' ;

basicVar : entVar | ('var' '(' (varName',' count 
					| varName',' from',' to) ')')) ;				
					
basicVars : 'varType' '('(basicVar',')* basicVar ')' ;

typeVars : 'varType' '('(basicVar',')+ basicVar ')' ;

varType : 'inputVars' | 'outputVars' | 'linkedVars' | 'constantVars' ;

mdlObjectiveVars :	'objectiveVars' '('objectiveVar*','objectiveVar ')' ;

mdlConstraintVars :	'constraintVars' '('constraintVar*','constraintVar ')' ;

	
modelingInstance: 'instance' '('bldrSignature')' ; 

mdlParametricTable : 'parametricTable' '(' (( tableURL | filename) (',' tableSeparator)?  | dataTable | instanceofModelTable'.class')')' ;

dataTable : 'table' '(' 'header' '('(varName ',')* varName')' (',' 'row' '(' ( value ',')* value')')* ')' ;	
 
mdlResponseTable : 'responseTable' '(' (filename |tableURL) (',' tableSeparator)? ')' ;
 
entVar : 'var' '('(varName ',')? contextEntry ')' ;
 
objectiveVar : 'var' '('varName',' outputVarName',' optiTarget ')' ;

optiTarget : 'Target.min' | 'Target.max' ;

constraintVar : 'var' '('varName',' outputVarName',' 'Relation.'relationSuffix ')' ;
 	
relationSuffix: 'lt' | 'lte' | 'eq' | 'gt' | 'gte' ;

varRealization : 'realization' '('varName',' 'fi' '('fiName',' varComponent+')'* ',' 
						'fi' '('fiName',' 'differentiation' '(' 'wrt' '('varName+')' ')'* ')' ;

varComponent : 'evaluator' '('evaluatorName')' | 'getter' '('getterName')' | 'setter' '('setterName')' ;

/* <VAR-ORIENTED-MODELING-TASKS> */

modelingTask : mdlResponseTask | mdlParamericTask | mdlOptimizationTask ;

mdlResponseTask : 'responseTask' '(' 'outerSig' '('selector',' mdlSig ')'(',' responseContext ')')? ')' ;
		
mdlParamericTask : 'parametricTask' '(' 'outerSig' '(' selector',' mdlSig')' ',' paramContext ')' ;

mdlOptimizationTask : 'optimizationTask' '('explorerSignature',' optiContext ',' optiStrategy ')' ;
			
responseContext : 'modelingContext' '(' (mdlInputs',')? (mdlResponses',')? returnPath? ')';

paramContext : 'modelingContext' '(' mdlParametricTable',' (mdlResponseTable',')? (mdlParmeters',')?  (mdlResponses',')?  parStrategy? (',' returnPath)?')'; 

optiContext : 'modelingContext' '(' mdlInputs (',' returnPath)?')'; 
	
mdlInputs : 'inputs' '('((dataEntry ',')* dataEntry)')'; 
	 
mdlResponses : 'responses' '('((varName ',')* varName)')';

returnPath : 'result' '('pathName')';

outTable : 'table' '(' mdlParametricTable(',' mdlResponseTable)?')';
	
parStrategy : 'parallel' '(' 'queue' '('int')'',' 'pool' '('int')' ')';

mdlParmeters : 'parameters' '('((varName ',')* varName)')'; 
	 						
optiStrategy : 'strategy' '(' optiTarget',' dispatchSig',' mdlSig',' optiSig ')';

dispatchSig : 'dispatcherSig' '('prvSignature')';

mdlSig : 'modelSig' '('prvSignature')';

optiSig : 'optimizerSig' '('prvSignature')';

explorerSignature : opSignature;


/* <ACCESSING-VALUES> */

contextValueResult : ('value' '('dataContext',' ( pathName | outputPaths ) 
			| 'valueAt' '('dataContext',' index 
			| ('valueAt' | 'valuesAt') '('dataContext',' pathTag )')' ;

srvValueResult : (('exec' '(' srvRequest | 
			'eval' '('contextEntry | 'eval' '('entModel',' pathName
			| 'eval' '('srvExertion )(',' srvArg)* | 'returnValue' '(' srvMogram 
			| 'get' '(' contextModel ',' pathName)')' ; 

srvMogramResult : 'exert' '('srvMogram (',' srvArg)*')' ;

dataContextResult : ('response' '('entModel (',' srvArg)* 
			| 'result' '('entModel (',' pathName)? | 'context' '(' srvMogram 
			| 'upcontext' '('compoundExertion ) ')' ;

srvEexrtionResult : 'get' '('srvExertion',' componentPathName')' ;

contextEntryResult : ('getEntry''('contextModel',' pathName |  'setValue' '(' contextEntry value ) ')' ;

contextModelResult : ('setValue''('contextModel',' pathName',' value 
	| 'setValue''('contextModel',' (contextEntry ',')* contextEntry 
	|  'append''('contextModel',' contextModel ) ')' ;
	
contextSnapshotResult : ( 'snapshot''(' structuredVarModel ',' responseContext? )')'  ;
			
		
varInformation  : 'varInfo''('varsType (',' varName)*')' ;

varsType : 'INPUTS' | 'CONSTANTS' | 'INVARIANTS' | 'DESIGN' | 'PARAMETERS'
			| 'ALL_OUTPUTS' | 'OUTPUTS' | 'RESPONSES' | 'LINKED' | 'OBJECTIVES'
			| 'CONSTRAINTS' | 'WATCHABLE' | 'ALL' | 'NONE' | 'NULL' ;

srvArg : (opSignature | dataEntry | srvMogram | fiSelector | metaFiSelector | cxtSelector
            | inputPaths | outputPaths | srvResult | opSignature | instanceofArg'.class') ;


/*<END>*/


/* Concrete labels */
argName   : name;
classpath : name;
componentName : name;
configName    : name;
fiName        : name ;
dependency    : name ;
dependentVarName : name ;
parameterName    : name ;
groupName 	     : name ;
getterName 	     : name;
setterName 	     : name;
tableSeparator   : name;
tableURL : url;
url      : 'file://'name | 'http://'name | 'https://'name | 'artifact:'name | 'sos:'name ;

double : intNumber ;
min : int;
max : int;
index : int ;
count : int;
from : int;
to : int;
int : intNumber ;
intNumber : ('0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9') ;

jarName : name;
locatorName :	 name ;
fromPathName : name ;
toPathName : name ;
pathName :	 name ;
componentPathName : name ;	
pathTag :	 name ;
newTagAssociation :	 name;
lambdaEvaluator :	 name;
contextPathName	:	 name ;
sigFi :	 name ;
entEvaluator :	 name;
objectImplementingEvaluation :	 name;

evaluatorName :	 name;

compFiSelector : name;

contextModelParameters : name;
srvExertionParamters : name;

conditionCallableLambda : name;
conditionExpression : name;
javaExpression	: name;

objectImplementingInvocation : name;
objectImplementingGetter :	name;
objectImplementingSetter :	name;

entrycallableLambdaExpression : name;
serviceLambdaExpression       : name;
callableLambdaExpression      : name;
clientLambdaExpression        : name;
valueCallableLambdaExpression : name;
morpherLambdaExpression       : name;
instanceofModelTable 	 : name ;

filename 	: name ;
netletFilename	: name ;
netletArtifact  : name ;
toPath       	: name ;
fromPath       	: name ;

prvClassName 	  : name ;
providerName	  : name ;	
sigName	  	  : name ;	
selector          : name ;
classSelector     : name ;
serviceName       : name ;
modelName 	  : name ;
varName 	  : name ;
outputVarName	  : name ;
srvTypeName       : name ;	 
value 	          : name ;	
object 		: name ;
instanceofArg   : name ;
class 		: name ;
interface 	: name;
classType 	: name'.class' ;
interfaceType 	: name'.class' ;
name            : ID ;
string_literal       : '"'ID'"' ;

ID                   : ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '0'..'9' | '_')* ;
