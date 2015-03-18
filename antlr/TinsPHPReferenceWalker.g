/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

/* 
 * This file was created based on TSPHPReferenceWalker.g - the tree grammar file for the reference phase of
 * TSPHP's type checker - and reuses TSPHP's AST class as well as other classes/files related to the AST generation.
 * TSPHP is also licenced under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

tree grammar TinsPHPReferenceWalker;
options {
    tokenVocab = TinsPHP;
    ASTLabelType = ITSPHPAst;
}

@header{
/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.antlr;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPAstAdaptor;
import ch.tsphp.common.ITSPHPErrorAst;
import ch.tsphp.tinsphp.common.scopes.ICaseInsensitiveScope;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.common.inference.IReferencePhaseController;
}

@members {
private IReferencePhaseController controller;
private ITSPHPAstAdaptor adaptor;
private boolean hasAtLeastOneReturnOrThrow;
private boolean doesNotReachThisStatement;
private boolean inSwitch;

public TinsPHPReferenceWalker(TreeNodeStream input, IReferencePhaseController theController, ITSPHPAstAdaptor theAdaptor) {
    this(input);
    controller = theController;
    adaptor = theAdaptor;
}
}

compilationUnit
    :   namespace+
    ;
    
namespace
    :   ^(Namespace 
        {
            controller.transferInitialisedSymbolsFromGlobalDefault($Namespace);
        }
            (TYPE_NAME|DEFAULT_NAMESPACE) 
            n=namespaceBody
        )
        {
            controller.transferInitialisedSymbolsToGlobalDefault($Namespace);
        }
    ;    

namespaceBody
    :   ^(NAMESPACE_BODY statement*)
    |   NAMESPACE_BODY
    ;

statement
    :   useDefinitionList
    |   definition
    |   instruction
    ;

useDefinitionList
    :   ^(Use useDeclaration+)
    ;
    
useDeclaration
    :   ^(USE_DECLARATION typeName=TYPE_NAME alias=Identifier)
        {
            ITypeSymbol typeSymbol = controller.resolveUseType($typeName, $alias);
            $typeName.setSymbol(typeSymbol);
            $alias.getSymbol().setType(typeSymbol);
            
            controller.checkUseDefinition($alias);
        }
    ;

definition
    :   //TODO TINS-210 - reference phase - class definitions
        //classDefinition
        // TINS-211 - reference phase - interface definitions
    //|   interfaceDefinition
        functionDefinition
    |   constDefinitionList
    ;
    
//TODO TINS-210 - reference phase - class definitions
/*    
classDefinition
    :   ^(Class
            cMod=.
            identifier=Identifier 
            classExtendsDeclaration[$identifier] 
            implementsDeclaration[identifier] 
            classBody) 
        {
            //TODO TINS-216 reference phase - double definition check classes
            //INamespaceScope namespaceScope = (INamespaceScope) $identifier.getScope();
            //namespaceScope.doubleDefinitionCheckCaseInsensitive($identifier.getSymbol());
        }
    ;

classExtendsDeclaration[ITSPHPAst identifier]
    :   ^(Extends classInterfaceType[null])
        {
            ITypeSymbol typeSymbol = $classInterfaceType.type;
            if(controller.checkIsClass($classInterfaceType.start, typeSymbol)){
                IClassTypeSymbol classTypeSymbol = (IClassTypeSymbol) identifier.getSymbol();
                classTypeSymbol.setParent((IClassTypeSymbol)typeSymbol);
                classTypeSymbol.addParentTypeSymbol((IClassTypeSymbol)typeSymbol);
            }
        } 
    |   'extends'
    ;

implementsDeclaration[ITSPHPAst identifier]
    :   ^('implements'
            (classInterfaceType[null]{
                ITypeSymbol typeSymbol = $classInterfaceType.type;
                if(controller.checkIsInterface($classInterfaceType.start, typeSymbol)){
                    ((IClassTypeSymbol)identifier.getSymbol()).addParentTypeSymbol((IInterfaceTypeSymbol)typeSymbol);
                }
            })+
        )
    |   'implements'
    ;
    
classBody
    :   ^(CLASS_BODY classBodyDefinition*)
    |   CLASS_BODY
    ;
    
classBodyDefinition
    :   constDefinitionList
    |   fieldDefinition
    |   constructDefinition
    |   methodDefinition
    ;
*/

constDefinitionList
    :   ^(CONSTANT_DECLARATION_LIST 
            ^(TYPE tMod=. scalarTypesOrUnknown[tMod]) 
            constDeclaration[$scalarTypesOrUnknown.type]+
        )
    ;

constDeclaration[ITypeSymbol type]
    :   ^(identifier=Identifier unaryPrimitiveAtom)
        {
            $identifier.getSymbol().setType(type);
            if(!controller.checkIsNotDoubleDefinition($identifier)){
              //TODO flag double definitions in order that output component can decide how to proceed
            }
        }
    ;

unaryPrimitiveAtom
    :   primitiveAtomWithConstant
    |   ^(UNARY_MINUS primitiveAtomWithConstant)
    |   ^(UNARY_PLUS primitiveAtomWithConstant)
    ; 
    
primitiveAtomWithConstant
@init{
    ITypeSymbol typeSymbol = null;
}
@after{
     $start.setEvalType(typeSymbol);
}
    :   (   type=Null
        |   type=False
        |   type=True
        |   type=Int
        |   type=Float
        |   type=String
        |   array {$type=$array.start;}
        )
        {
           typeSymbol = controller.resolvePrimitiveLiteral($type);
        }

    |   cnst=CONSTANT
        {
            IVariableSymbol variableSymbol = controller.resolveConstant($cnst);
            $cnst.setSymbol(variableSymbol);
            if(controller.checkIsNotForwardReference($cnst)){
                typeSymbol = variableSymbol.getType();
            }else{
                String constName = $CONSTANT.text;
                ITSPHPAst ast = (ITSPHPAst) adaptor.create(this.String, $CONSTANT.getToken(), constName.substring(0,constName.length()-1));
                typeSymbol = controller.resolvePrimitiveLiteral(ast);
            }
        }
    
    //TODO TINS-217 reference phase - class constant access
    //|   ^(CLASS_STATIC_ACCESS accessor=staticAccessor identifier=CONSTANT)
    //    {$identifier.setSymbol(accessResolver.resolveClassConstantAccess($accessor.start, $identifier));}
    ;

array
    :   ^(TypeArray arrayKeyValue*)
    ;

arrayKeyValue
    :   ^('=>' expression expression)
    |   value=expression
    ;

//TODO TINS-217 reference phase - class constant access
/*
staticAccessor
@after{$start.setEvalType((ITypeSymbol) $start.getSymbol());}
    :   classInterfaceType[null]
    |   slf='self'
        {
            IVariableSymbol variableSymbol = controller.resolveThisSelf($slf);
            $slf.setSymbol(variableSymbol.getType());
        }
    |   par='parent'
        {
            IVariableSymbol variableSymbol = controller.resolveParent($par);
            $par.setSymbol(variableSymbol.getType());
        }    
    ;
*/
    
// TINS-220 - reference phase - double definition check fields
/*    
fieldDefinition
    :   ^(FIELD variableDeclarationList[true])
    ;

variableDeclarationList[boolean isImplicitlyInitialised] 
    :   ^(VARIABLE_DECLARATION_LIST
            ^(TYPE tMod=. allTypesOrUnknown[$tMod]) 
            variableDeclaration[$allTypesOrUnknown.type, isImplicitlyInitialised]+ 
        )
    ;

variableDeclaration[ITypeSymbol type, boolean isImplicitlyInitialised] returns [IVariableSymbol variableSymbol]
@init{boolean isInitialised = false;}
    :   (   ^(variableId=VariableId expression) {isInitialised = true;}
        |   variableId=VariableId
        )
        { 
            //Warning! start duplicated code as in parameterNormalOrOptional
            //$variableSymbol = (IVariableSymbol) $variableId.getSymbol();
            //$variableSymbol.setType(type); 
            //$variableId.getScope().doubleDefinitionCheck($variableId.getSymbol());
            //Warning! end duplicated code as in parameterNormalOrOptional
            //TODO Fields are always initially initialised. 
            // Maybe it is better to rename this rule to fieldDeclaration (for now, static variables or global variables within function could not be initialised)
            //if(isInitialised || isImplicitlyInitialised){
            //    $variableId.getScope().addToInitialisedSymbols($variableSymbol, true);
            //}
        }
    ;
*/

//TODO TINS-221 - reference phase - double definition check methods 
/*
constructDefinition
    :   ^(identifier='__construct'
            .
            ^(TYPE rtMod=. voidType) 
            parameterDeclarationList block[false]
        )
        {
            IMethodSymbol methodSymbol = (IMethodSymbol) $identifier.getSymbol();
            methodSymbol.setType($voidType.type); 
            ICaseInsensitiveScope scope = (ICaseInsensitiveScope) $identifier.getScope();
            scope.doubleDefinitionCheckCaseInsensitive(methodSymbol);
        }
    ;
*/
  
//TODO TINS-221 - reference phase - double definition check methods 
/*        
methodDefinition

//Warning! start duplicated code as in functionDefinition
    @init{
        hasAtLeastOneReturnOrThrow = false;
        boolean shallCheckIfReturns = false;
    
//Warning! end duplicated code as in functionDefinition

    :   ^(METHOD_DECLARATION
            ^(METHOD_MODIFIER methodModifier)
            ^(TYPE rtMod=. returnTypes[$rtMod]) 
            {shallCheckIfReturns = !($returnTypes.type instanceof IVoidTypeSymbol) && !$methodModifier.isAbstract;}
            (identifier=Identifier|identifier=Destruct) parameterDeclarationList block
        )
        {
        //Warning! start duplicated code as in functionDeclaration
            IMethodSymbol methodSymbol = (IMethodSymbol) $identifier.getSymbol();
            methodSymbol.setType($returnTypes.type); 
            ICaseInsensitiveScope scope = (ICaseInsensitiveScope) $identifier.getScope();
            scope.doubleDefinitionCheckCaseInsensitive(methodSymbol);
            if(shallCheckIfReturns){
        //Warning! end duplicated code as in functionDeclaration
                controller.checkReturnsFromMethod($block.isReturning, hasAtLeastOneReturnOrThrow, $identifier);
            }
        }

    ;    

methodModifier returns[boolean isAbstract]
    :   (   Static  Final           accessModifier
        |   Static  accessModifier  Final
        |   Static  accessModifier

        |   Final   Static          accessModifier
        |   Final   accessModifier  Static
        |   Final   accessModifier
        
        
        |   accessModifier  Final   Static
        |   accessModifier  Static  Final
        |   accessModifier  Static
        |   accessModifier  Final
        |   accessModifier
        
        |    abstr=Abstract accessModifier
        |    accessModifier abstr=Abstract
        )
        {$isAbstract= $abstr != null;}
    ;

accessModifier
    :   Private
    |   Protected
    |   Public
    ;
*/

    
functionDefinition

//Warning! start duplicated code as in methodDefinition
    @init{
        boolean tmpDoesNotReachThisStatement = doesNotReachThisStatement;
        //defined above as field
        hasAtLeastOneReturnOrThrow = false;
    }
//Warning! start duplicated code as in methodDefinition
    :   ^('function'
            .
            ^(TYPE rtMod=. returnTypesOrUnknown[$rtMod])  
            identifier=Identifier parameterDeclarationList block
        )
        {
        //Warning! start duplicated code as in functionDeclaration
            $identifier.getSymbol().setType($returnTypesOrUnknown.type);
            controller.checkIsNotDoubleDefinitionCaseInsensitive($identifier);
        //Warning! end duplicated code as in functionDeclaration
            controller.addImplicitReturnStatementIfRequired(
            $block.isReturning, hasAtLeastOneReturnOrThrow, $identifier, $block.start);
        }
    ;
finally{
    doesNotReachThisStatement = tmpDoesNotReachThisStatement;
}

parameterDeclarationList
    :   ^(PARAMETER_LIST parameterDeclaration*)
    ;

parameterDeclaration
    :   ^(PARAMETER_DECLARATION
            ^(TYPE tMod=. allTypesOrUnknown[$tMod]) 
            (   variableId=VariableId
            |   ^(variableId=VariableId unaryPrimitiveAtom)
            )
        )
        {
            //Warning! start duplicated code as in variableDeclaration
            IVariableSymbol variableSymbol = (IVariableSymbol) $variableId.getSymbol();
            variableSymbol.setType($allTypesOrUnknown.type); 
            controller.checkIsNotDoubleDefinition($variableId);
            //Warning! end duplicated code as in variableDeclaration

            IMethodSymbol methodSymbol = (IMethodSymbol) $variableId.getScope();
            methodSymbol.addParameter(variableSymbol);
            methodSymbol.addToInitialisedSymbols(variableSymbol, true);
        }
    ;

block returns[boolean isReturning]
    :   ^(BLOCK instructions) {$isReturning = $instructions.isReturning;}
    |   BLOCK {$isReturning = false;}
    ;

    
// TINS-211 - reference phase - interface definitions
/*
interfaceDefinition
    :   ^('interface' iMod=. identifier=Identifier extIds=interfaceExtendsDeclaration[$identifier] interfaceBody)
        {
            INamespaceScope namespaceScope = (INamespaceScope) $identifier.getScope();
            namespaceScope.doubleDefinitionCheckCaseInsensitive($identifier.getSymbol());
        }
    ;
interfaceExtendsDeclaration[ITSPHPAst identifier]
    :   ^('extends'
            (allTypes[null]{
                ITypeSymbol typeSymbol = $allTypes.type;
                if(controller.checkIsInterface($allTypes.start, typeSymbol)){
                    ((IInterfaceTypeSymbol)identifier.getSymbol()).addParentTypeSymbol((IInterfaceTypeSymbol)typeSymbol);
                }
            })+
        ) 
    |   'extends'
    ;
    
interfaceBody
    :   ^(INTERFACE_BODY interfaceBodyDefinition*)
    |   INTERFACE_BODY
    ;
    
interfaceBodyDefinition
    :   constDefinitionList
    |   methodDefinition
    |   constructDefinition
    ;    
*/

instructions returns[boolean isReturning]
@init{boolean isBreaking = false;}
    :   (   instruction
            {
                $isReturning = $isReturning || (!isBreaking && $instruction.isReturning);
                isBreaking = isBreaking || $instruction.isBreaking;
            }
        )+
    ;

instruction returns[boolean isReturning, boolean isBreaking]
    // those statement which do not have an isReturning block can never return. 
    // Yet, it might be that they contain a return or throw statement and thus
    // hasAtLeastOneReturnOrThrow has been set to true
    :   ifCondition                  {$isReturning = $ifCondition.isReturning;}
    |   switchCondition              {$isReturning = $switchCondition.isReturning;}
    |   forLoop
    |   foreachLoop
    |   whileLoop
    |   doWhileLoop                  {$isReturning = $doWhileLoop.isReturning;}
    |   tryCatch                     {$isReturning = $tryCatch.isReturning;}
    |   ^(EXPRESSION expression?)
    |   ^('return' expression?)      {$isReturning = true; hasAtLeastOneReturnOrThrow = true; doesNotReachThisStatement = true;}
    |   ^('throw' expression)        {$isReturning = true; hasAtLeastOneReturnOrThrow = true; doesNotReachThisStatement = true;}
    |   ^('echo' expression+)
    |   breakContinue                {$isBreaking = true; doesNotReachThisStatement = inSwitch;}
    ;
    
ifCondition returns[boolean isReturning]
    :   ^('if'
            expression 
            ifBlock=blockConditional
            (elseBlock=blockConditional)?
        )
        {
            $isReturning = $ifBlock.isReturning && $elseBlock.isReturning;
            if($isReturning){
                doesNotReachThisStatement = true;
            }
            controller.sendUpInitialisedSymbolsAfterIf($ifBlock.ast, $elseBlock.ast);
        }
    ;

blockConditional returns[boolean isReturning, ITSPHPAst ast]
@init{
    boolean tmpdoesNotReachThisStatement = doesNotReachThisStatement;
}
    :   ^(BLOCK_CONDITIONAL instructions)
        {
            $isReturning = $instructions.isReturning; 
            $ast = $BLOCK_CONDITIONAL;
        }
        
    |   BLOCK_CONDITIONAL
        {
            $isReturning = false; 
            $ast = $BLOCK_CONDITIONAL;
        }
    ;
finally{
    doesNotReachThisStatement = tmpdoesNotReachThisStatement;
}
    
switchCondition returns[boolean isReturning]
@init{
    boolean tmpInSwitch = inSwitch;
    inSwitch = true;
}
    :   ^('switch' expression switchContents?)
        {
            $isReturning = $switchContents.hasDefault && $switchContents.isReturning;
            if($isReturning){
                doesNotReachThisStatement = true;
            }
        }
    ;
finally{
    inSwitch = tmpInSwitch;
}

    
switchContents returns[boolean isReturning, boolean hasDefault]
//Warning! start duplicated code as in catchBlocks
@init{
    boolean isFirst = true;
    List<ITSPHPAst> asts = new ArrayList<>();
}
//Warning! end duplicated code as in catchBlocks
    :   (   ^(SWITCH_CASES caseLabels) blockConditional
            {
                $hasDefault = $hasDefault || $caseLabels.hasDefault;
                $isReturning = $blockConditional.isReturning && ($isReturning || isFirst);        
                isFirst = false;        
                asts.add($blockConditional.ast);
            }
        )+
        {controller.sendUpInitialisedSymbolsAfterSwitch(asts, $hasDefault);}
    ;

caseLabels returns[boolean hasDefault]
    :   (   expression
        |   Default {$hasDefault=true;}
        )+
    ;
    
forLoop
@init{
    boolean tmpInSwitch = inSwitch;
    inSwitch = false;
}
    :   ^('for'
            expressionList
            expressionList 
            expressionList
            blockConditional
        )
        {controller.sendUpInitialisedSymbols($blockConditional.ast);}
    ;
finally{
    inSwitch = tmpInSwitch;
}

expressionList
    :   ^(EXPRESSION_LIST expression*)
    ;

foreachLoop
@init{
    boolean tmpInSwitch = inSwitch;
    inSwitch = false;
}
    :
        ^(foreach='foreach' 
            expression 
            varId1=VariableId
            // Corresponding to the parser the first VariableId (the key) should be optional.
            // However, it does not matter here since both are just VariableId this way we can avoid an LL1 conflict
            varId2=VariableId?
            {
                IVariableSymbol variableSymbol = controller.resolveVariable($varId1);
                $varId1.setSymbol(variableSymbol);
                IScope scope = varId1.getScope();
                scope.addToInitialisedSymbols(variableSymbol, true);
                if($varId2!=null){
                    variableSymbol = controller.resolveVariable($varId2);
                    $varId2.setSymbol(variableSymbol);
                    scope.addToInitialisedSymbols(variableSymbol, true);
                }
            }
            blockConditional
        )
        {
            controller.sendUpInitialisedSymbols($blockConditional.ast);
            controller.sendUpInitialisedSymbols($foreach);
        }
    ;
finally{
    inSwitch = tmpInSwitch;
}

whileLoop
@init{
    boolean tmpInSwitch = inSwitch;
    inSwitch = false;
}
    :   ^('while' expression blockConditional)
        {controller.sendUpInitialisedSymbols($blockConditional.ast);}
    ;
finally{
    inSwitch = tmpInSwitch;
}

doWhileLoop returns[boolean isReturning]
@init{
    boolean tmpInSwitch = inSwitch;
    inSwitch = false;
}
    :   ^('do' block expression)
        {$isReturning = $block.isReturning;}
    ;
finally{
    inSwitch = tmpInSwitch;
}

tryCatch returns[boolean isReturning]
    :   ^('try' blockConditional catchBlocks)
        {
            $isReturning = $blockConditional.isReturning && $catchBlocks.isReturning;
            if($isReturning){
                doesNotReachThisStatement = true;
            }
            $catchBlocks.asts.add($blockConditional.ast);
            controller.sendUpInitialisedSymbolsAfterTryCatch($catchBlocks.asts);
        }
    ;
    
catchBlocks returns[boolean isReturning, List<ITSPHPAst> asts]
//Warning! start duplicated code as in switchContents
@init{
    boolean isFirst = true;
    $asts = new ArrayList<>();
}
//Warning! end duplicated code as in switchContents
    :   (   ^(Catch 
                classInterfaceType[null] 
                variableId=VariableId 
                {
                    IVariableSymbol variableSymbol = controller.resolveVariable($variableId);
                    $variableId.setSymbol(variableSymbol);
                    $variableId.getScope().addToInitialisedSymbols(variableSymbol, true);
                }
                blockConditional
            )
            {
                $classInterfaceType.start.setEvalType($classInterfaceType.type);
            
                $isReturning = $blockConditional.isReturning && ($isReturning || isFirst);
                isFirst = false;

                controller.sendUpInitialisedSymbols($blockConditional.ast);
                $asts.add($Catch);
            }
        )+ 
    ;
    
breakContinue
    :   Break
    |   ^(Break Int)
    |   Continue
    |   ^(Continue Int)
    ;
    
expression
    :   atom
    |   operator
        {
            ITSPHPAst operator = $operator.start;
            operator.setSymbol(controller.resolveOperator(operator));
        }
    |   functionCall
    //TODO rstoll TINS-161 inference OOP    
    //|   methodCall
    //|   methodCallStatic
    //|   classStaticAccess
    |   postFixExpression
    |   exit
    ;
        
atom
    :   primitiveAtomWithConstant
    |   variable
    //TODO rstoll TINS-223 - reference phase - resolve this and self
    //|   thisVariable
    ;

variable    
    :   varId=VariableId
        {
            $varId.setSymbol(controller.resolveVariable($varId));
            if(!controller.checkIsVariableInitialised($varId)){
              //TODO use null instead of the variable
            }
        }
    ;
    
//TODO TINS-223 - reference phase - resolve this and self
/*    
thisVariable
    :   t='$this'
        {$t.setSymbol(controller.resolveThisSelf($t));}
    ;
*/

operator
    :   ^(unaryOperator expression)
    |   ^(binaryOperatorExcludingAssign expression expression)
    |   ^(assignOperator varId=expression expression)
        {
            ITSPHPAst variableId = $varId.start;
            if(!doesNotReachThisStatement && variableId.getType()==VariableId){
                variableId.getScope().addToInitialisedSymbols(variableId.getSymbol(), true);
            }
        }
    |   ^('?' expression expression expression)
    |   ^(CAST ^(TYPE tMod=. scalarTypesOrArrayType[$tMod]) expression)
    |   ^(Instanceof expr=expression (variable|classInterfaceType[null]))
    //|   ^('new' classInterfaceType[null] actualParameters)
    |   ^('clone' expression)
    ;
        
unaryOperator
    :   PRE_INCREMENT
    |   PRE_DECREMENT
    |   '@'
    |   '~'
    |   '!'
    |   UNARY_MINUS
    |   UNARY_PLUS
    |   POST_INCREMENT
    |   POST_DECREMENT
    ;
    
binaryOperatorExcludingAssign
// operators can be overloaded and thus type information is needed to resolve them
// they are resolved during the inference phase
    :   'or'
    |   'xor'
    |   'and'
    
    |   '||'
    |   '&&'
    |   '|'
    |   '^'
    |   '&'
    
    |   '=='
    |   '!='
    |   '==='
    |   '!=='
    
    |   '<'
    |   '<='
    |   '>'
    |   '>='
    
    |   '<<'
    |   '>>'
    
    |   '+'
    |   '-'
    |   '.'
    
    |   '*'
    |   '/'
    |   '%'
    ;
    
assignOperator
// operators can be overloaded and thus type information is needed to resolve them
// they are resolved during the inference phase
    :   '='
    |   '+='
    |   '-='
    |   '*='
    |   '/='
    |   '&='
    |   '|='
    |   '^='
    |   '%='
    |   '.='
    |   '<<='
    |   '>>='
    ;
    
functionCall
        // function call has no callee and is therefor not resolved in this phase.
        // resolving occurs in the inference phase where overloads are taken into account
    :   ^(FUNCTION_CALL identifier=TYPE_NAME actualParameters)
    	{
            $identifier.setSymbol(controller.resolveFunction($identifier));
    	}
    ;
    
actualParameters
    :   ^(ACTUAL_PARAMETERS expression+)
    |   ACTUAL_PARAMETERS
    ; 
  
//TODO TINS-161 inference OOP    
/*
methodCall
    :   ^(METHOD_CALL methodCallee Identifier actualParameters)
    ;
    
methodCallee
    :  //TODO TINS-223 reference phase - resolve this and self
       // thisVariable
       variable
    //TODO TINS-223 reference phase - resolve this and self
    //|   slf='self'
    //    {$slf.setSymbol(controller.resolveThisSelf($slf));}
    //TODO TINS-225 - reference phase - resolve parent
    //|   par='parent'
    //    {$par.setSymbol(controller.resolveParent($par));}    
    ;
    
methodCallStatic
    :   ^(METHOD_CALL_STATIC classInterfaceType[null] Identifier actualParameters)
    ;
    
classStaticAccess
    :   ^(CLASS_STATIC_ACCESS accessor=staticAccessor identifier=CLASS_STATIC_ACCESS_VARIABLE_ID)
        {$identifier.setSymbol(accessResolver.resolveStaticFieldAccess($accessor.start, $identifier));}
    ;        
*/
postFixExpression
// postFixExpression are resolved in the type checking phase
// due to the fact that method/function calls are resolved during the type check phase
// This rules are needed to resolve variables/function calls etc. in expression and actualParameters
    :   //TODO TINS-161 inference OOP
        //^(FIELD_ACCESS expression Identifier)
       ^(ARRAY_ACCESS expression expression)
    //TODO TINS-161 inference OOP
    //|   ^(METHOD_CALL_POSTFIX expression Identifier actualParameters)
    ;

exit
    :   ^('exit' expression)
    |   'exit'
    ;

returnTypesOrUnknown[ITSPHPAst typeModifier] returns [ITypeSymbol type]
        //PHP does not allow to specify return types so far, hence only the unkown type is possible
    :   '?' {$type = null;}
    ;    
    
allTypesOrUnknown[ITSPHPAst typeModifier] returns [ITypeSymbol type]
    :   scalarTypes[$typeModifier] {$type = $scalarTypes.type;}
    |   classInterfaceType[$typeModifier] {$type = $classInterfaceType.type;}
    |   arrayType[$typeModifier] {$type = $arrayType.type;}
    //unknown type - needs to be inferred during the inference phase
    |   '?' {$type = null;}
    ;
    
scalarTypesOrArrayType[ITSPHPAst typeModifier] returns [ITypeSymbol type]
    :   scalarTypes[typeModifier] {$type = $scalarTypes.type;}
    |   arrayType[$typeModifier] {$type = $arrayType.type;}
    ;

scalarTypesOrUnknown[ITSPHPAst typeModifier] returns [ITypeSymbol type]
    :   scalarTypes[typeModifier] {$type = $scalarTypes.type;}
    |   '?' {$type = null;}
    ;
    
scalarTypes[ITSPHPAst typeModifier] returns [ITypeSymbol type]
//Warning! start duplicated code as in voidType, classInterfaceType and arrayOrResourceOrMixed
@init{
    if(state.backtracking == 1 && $start instanceof ITSPHPErrorAst){
        $type = controller.createErroneousTypeSymbol((ITSPHPErrorAst)$start);
        $start.setSymbol($type);        
        input.consume();
        return retval;
    }
}
//Warning! end duplicated code as in voidType, classInterfaceType and arrayOrResourceOrMixed
    :   (   'bool'
        |   'int'
        |   'float'
        |   'string'
        )
        {
            $type = controller.resolvePrimitiveType($start, $typeModifier);
            $start.setSymbol($type);
        }
    ;
catch[RecognitionException re]{
    reportError(re);
    recover(input,re);
    $type = controller.createErroneousTypeSymbol($start, re);
}
    
classInterfaceType[ITSPHPAst typeModifier] returns [ITypeSymbol type]
//Warning! start duplicated code as in scalarTypes and arrayOrResourceOrMixed
@init{
    if(state.backtracking == 1 && $start instanceof ITSPHPErrorAst){
        $type = controller.createErroneousTypeSymbol((ITSPHPErrorAst)$start);
        $start.setSymbol($type);        
        input.consume();
        return retval;
    }
}
//Warning! end duplicated code as in scalarTypes and arrayOrResourceOrMixed
    :   TYPE_NAME
        {
            $type = controller.resolveType($start, $typeModifier);
            $start.setSymbol($type);
        }
    ;
catch[RecognitionException re]{
    reportError(re);
    recover(input,re);
    $type = controller.createErroneousTypeSymbol($start, re);
}
    
arrayType[ITSPHPAst typeModifier] returns [ITypeSymbol type]
//Warning! start duplicated code as in scalarTypes, classInterfaceType
@init{
    if(state.backtracking == 1 && $start instanceof ITSPHPErrorAst){
        $type = controller.createErroneousTypeSymbol((ITSPHPErrorAst)$start);
        $start.setSymbol($type);
        input.consume();
        return retval;
    }
}
//Warning! end duplicated code as in scalarTypes, classInterfaceType
    :   'array'
        {
            $type = controller.resolvePrimitiveType($start, $typeModifier);
            $start.setSymbol($type);
        }
    ;
catch[RecognitionException re]{
    reportError(re);
    recover(input,re);
    $type = controller.createErroneousTypeSymbol($start, re);
}