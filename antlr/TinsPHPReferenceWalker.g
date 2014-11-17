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
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.common.ITSPHPErrorAst;
import ch.tsphp.tinsphp.common.scopes.ICaseInsensitiveScope;
import ch.tsphp.tinsphp.common.symbols.IAliasSymbol;
import ch.tsphp.tinsphp.common.symbols.IMethodSymbol;
import ch.tsphp.tinsphp.common.symbols.IVariableSymbol;
import ch.tsphp.tinsphp.inference_engine.IReferencePhaseController;
}

@members {
private IReferencePhaseController controller;
private boolean hasAtLeastOneReturnOrThrow;

public TinsPHPReferenceWalker(TreeNodeStream input, IReferencePhaseController theController) {
    this(input);
    controller = theController;
}
}

compilationUnit
    :   namespace+
    ;
    
namespace
    :   ^(Namespace . n=namespaceBody)
    ;    

namespaceBody
    :   ^(NAMESPACE_BODY statement*)
    |   NAMESPACE_BODY
    ;

statement
    :   useDefinitionList
    |   definition
    |   instruction[false]
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
            
            controller.useDefinitionCheck((IAliasSymbol) $alias.getSymbol());
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
            IVariableSymbol variableSymbol = (IVariableSymbol) $identifier.getSymbol();
            variableSymbol.setType(type); 
            $identifier.getScope().doubleDefinitionCheck(variableSymbol); 
        }
    ;

unaryPrimitiveAtom
    :   primitiveAtomWithConstant
    |   ^(  (    unary=UNARY_MINUS
            |    unary=UNARY_PLUS
            ) primitiveAtomWithConstant
        )
    ; 
    
//TODO TINS-218 - reference phase - resolve primitive literals    
primitiveAtomWithConstant
    :   Bool
    |   Int
    |   Float
    |   String
    |   Null
    |   array
    |   cnst=CONSTANT
        {
            IVariableSymbol variableSymbol = controller.resolveConstant($cnst);
            $cnst.setSymbol(variableSymbol);
            controller.checkIsNotForwardReference($cnst);
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
*/

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
            //TODO TINS-208 reference phase - resolve variables    
            //$variableSymbol = (IVariableSymbol) $variableId.getSymbol();
            //$variableSymbol.setType(type); 
            //$variableId.getScope().doubleDefinitionCheck($variableId.getSymbol());
            //Warning! end duplicated code as in parameterNormalOrOptional
            //TODO TINS-219 - reference phase - check are variables initialised
            //if(isInitialised || isImplicitlyInitialised){
            //    $variableId.getScope().addToInitialisedSymbols($variableSymbol, true);
            //}
        }
    ;

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
//Warning! start duplicated code as in functionDeclaration
    @init{
        hasAtLeastOneReturnOrThrow = false;
        boolean shallCheckIfReturns = false;
    }
//Warning! end duplicated code as in functionDeclaration

    :   ^(METHOD_DECLARATION
            ^(METHOD_MODIFIER methodModifier)
            ^(TYPE rtMod=. returnTypes[$rtMod]) 
            {shallCheckIfReturns = !($returnTypes.type instanceof IVoidTypeSymbol) && !$methodModifier.isAbstract;}
            (identifier=Identifier|identifier=Destruct) parameterDeclarationList block[shallCheckIfReturns]
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
//Warning! start duplicated code as in functionDeclaration
    @init{
        //defined above as field
        hasAtLeastOneReturnOrThrow = false;
    }
//Warning! start duplicated code as in functionDeclaration
    :   ^('function'
            .
            ^(TYPE rtMod=. returnTypesOrUnknown[$rtMod])  
            identifier=Identifier parameterDeclarationList block[true]
        )
        {
        //Warning! start duplicated code as in functionDeclaration
            IMethodSymbol methodSymbol = (IMethodSymbol) $identifier.getSymbol();
            methodSymbol.setType($returnTypesOrUnknown.type); 
            ICaseInsensitiveScope scope = (ICaseInsensitiveScope) $identifier.getScope();
            scope.doubleDefinitionCheckCaseInsensitive(methodSymbol);
        //Warning! end duplicated code as in functionDeclaration
            controller.addImplicitReturnStatementIfRequired(
                $block.isReturning, hasAtLeastOneReturnOrThrow, $identifier, $block.start);
        }
    ;

parameterDeclarationList
    :   ^(PARAMETER_LIST parameterDeclaration+)
    |   PARAMETER_LIST
    ;

parameterDeclaration
    :   ^(PARAMETER_DECLARATION
            ^(TYPE tMod=. allTypesOrUnknown[$tMod]) 
            parameterNormalOrOptional[$allTypesOrUnknown.type]
        )
        //TODO TINS-208 reference phase - resolve variables
        /*{
            IVariableSymbol parameter = $parameterNormalOrOptional.variableSymbol;
            IMethodSymbol methodSymbol = (IMethodSymbol) parameter.getDefinitionScope();
            methodSymbol.addParameter(parameter);
        }*/
    ;

parameterNormalOrOptional[ITypeSymbol type] returns [IVariableSymbol variableSymbol]
    :   (    variableId=VariableId
        |    ^(variableId=VariableId unaryPrimitiveAtom)
        )
        { 
            //Warning! start duplicated code as in variableDeclaration
            $variableSymbol = (IVariableSymbol) $variableId.getSymbol();
            $variableSymbol.setType(type); 
            //TODO TINS-227 - reference phase - double definition check parameters
            //$variableId.getScope().doubleDefinitionCheck($variableId.getSymbol());
            //Warning! end duplicated code as in variableDeclaration
            //TODO TINS-219 reference phase - check are variables initialised
            //$variableId.getScope().addToInitialisedSymbols($variableSymbol, true);
        } 
    ;

block[boolean shallCheckIfReturns] returns[boolean isReturning]
    :   ^(BLOCK instructions[$shallCheckIfReturns]) {$isReturning = $instructions.isReturning;}
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

instructions[boolean shallCheckIfReturns] returns[boolean isReturning]
@init{boolean isBreaking = false;}
    :   (   instruction[$shallCheckIfReturns]
            {
                if(shallCheckIfReturns){
                    $isReturning = $isReturning || (!isBreaking && $instruction.isReturning);
                    isBreaking = $instruction.isBreaking;
                }
            }
        )+
    ;

instruction[boolean shallCheckIfReturns] returns[boolean isReturning, boolean isBreaking]
    // those statement which do not have an isReturning block can never return. 
    // Yet, it might be that they contain a return or throw statement and thus
    // hasAtLeastOneReturnOrThrow has been set to true
    :   ifCondition[$shallCheckIfReturns]       {$isReturning = $ifCondition.isReturning;}
    |   switchCondition[$shallCheckIfReturns]   {$isReturning = $switchCondition.isReturning;}
    |   forLoop
    |   foreachLoop
    |   whileLoop
    |   doWhileLoop[$shallCheckIfReturns]       {$isReturning = $doWhileLoop.isReturning;}
    |   tryCatch[$shallCheckIfReturns]          {$isReturning = $tryCatch.isReturning;}
    |   ^(EXPRESSION expression?)
    |   ^('return' expression?)                 {$isReturning = true; hasAtLeastOneReturnOrThrow = true;}
    |   ^('throw' expression)                   {$isReturning = true; hasAtLeastOneReturnOrThrow = true;}
    |   ^('echo' expression+)
    |   breakContinue                           {$isBreaking = true;}
    ;
    
ifCondition[boolean shallCheckIfReturns] returns[boolean isReturning]
    :   ^('if'
            expression 
            ifBlock=blockConditional[$shallCheckIfReturns]
            (elseBlock=blockConditional[$shallCheckIfReturns])?
        )
        {
            $isReturning = shallCheckIfReturns && $ifBlock.isReturning && $elseBlock.isReturning;
            //TODO TINS-219 reference phase - check are variables initialised
            //controller.sendUpInitialisedSymbolsAfterIf($ifBlock.ast, $elseBlock.ast);
        }
    ;

blockConditional[boolean shallCheckIfReturns] returns[boolean isReturning, ITSPHPAst ast]
    :   ^(BLOCK_CONDITIONAL instructions[$shallCheckIfReturns])
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
    
switchCondition[boolean shallCheckIfReturns] returns[boolean isReturning]
    :   ^('switch' expression switchContents[$shallCheckIfReturns]?)
        {
            $isReturning = $switchContents.hasDefault && $switchContents.isReturning;
        }
    ;
    
switchContents[boolean shallCheckIfReturns] returns[boolean isReturning, boolean hasDefault]
//Warning! start duplicated code as in catchBlocks
@init{
    boolean isFirst = true;
    List<ITSPHPAst> asts = new ArrayList<>();
}
//Warning! start duplicated code as in catchBlocks
    :   (   ^(SWITCH_CASES caseLabels) blockConditional[$shallCheckIfReturns]
            {
                if(shallCheckIfReturns){
                    $hasDefault = $hasDefault || $caseLabels.hasDefault;
                    $isReturning = $blockConditional.isReturning && ($isReturning || isFirst);        
                    isFirst = false;        
                }
                //TODO TINS-219 reference phase - check are variables initialised
                //asts.add($blockConditional.ast);
            }
        )+
        //TODO TINS-219 reference phase - check are variables initialised
        //{controller.sendUpInitialisedSymbolsAfterSwitch(asts, $hasDefault);}
    ;

caseLabels returns[boolean hasDefault]
    :   (   expression
        |   Default {$hasDefault=true;}
        )+
    ;
    
forLoop
    :   ^('for'
            expressionList
            expressionList 
            expressionList
            blockConditional[false]
        )
        //TODO TINS-219 reference phase - check are variables initialised
        //{controller.sendUpInitialisedSymbols($blockConditional.ast);}
    ;

expressionList
    :   ^(EXPRESSION_LIST expression*)
    |   EXPRESSION_LIST
    ;

foreachLoop
    :
        ^(foreach='foreach' 
            expression 
            variableDeclarationList[true]
            // corresponding to the parser the first variableDeclarationList (the key) should be optional
            // however, it does not matter here since both are just variable declarations
            variableDeclarationList[true]? 
            blockConditional[false]
        )
        //TODO TINS-219 reference phase - check are variables initialised           
        //{
        //    controller.sendUpInitialisedSymbols($blockConditional.ast);
        //    controller.sendUpInitialisedSymbols($foreach);
        //}
    ;

whileLoop
    :   ^('while' expression blockConditional[false])
        //TODO TINS-219 reference phase - check are variables initialised
        //{controller.sendUpInitialisedSymbols($blockConditional.ast);}
    ;


doWhileLoop[boolean shallCheckIfReturns] returns[boolean isReturning]
    :   ^('do' block[$shallCheckIfReturns] expression)
        {$isReturning = $block.isReturning;}
    ;

tryCatch[boolean shallCheckIfReturns] returns[boolean isReturning]
    :   ^('try' blockConditional[$shallCheckIfReturns] catchBlocks[$shallCheckIfReturns])
        {
            $isReturning = shallCheckIfReturns && $blockConditional.isReturning && $catchBlocks.isReturning;
            //TODO TINS-219 reference phase - check are variables initialised
            //$catchBlocks.asts.add($blockConditional.ast);
            //controller.sendUpInitialisedSymbolsAfterTryCatch($catchBlocks.asts);
        }
    ;
    
catchBlocks[boolean shallCheckIfReturns] returns[boolean isReturning, List<ITSPHPAst> asts]
//Warning! start duplicated code as in switchContents
@init{
    boolean isFirst = true;
    $asts = new ArrayList<>();
}
//Warning! start duplicated code as in switchContents
    :   (   ^('catch' variableDeclarationList[true] blockConditional[$shallCheckIfReturns])
            {
                if(shallCheckIfReturns){
                    $isReturning = $blockConditional.isReturning && ($isReturning || isFirst);
                    isFirst = false;
                }
                //TODO TINS-219 reference phase - check are variables initialised
                //$asts.add($blockConditional.ast);
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
    |   functionCall
    //TODO TINS-161 inference OOP    
    //|   methodCall
    //|   methodCallStatic
    //|   classStaticAccess
    |   postFixExpression
    |   exit
    ;
        
atom
    :   
        primitiveAtomWithConstant
    |   variable
    //TODO TINS-223 - reference phase - resolve this and self
    //|   thisVariable
    ;

variable    
    :   varId=VariableId
        //TODO TINS-219 reference phase - check are variables initialised
        //{
        //    $varId.setSymbol(controller.resolveVariable($varId));
        //    controller.checkVariableIsOkToUse($varId);
        //}
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
        //TODO TINS-219 reference phase - check are variables initialised
        /*{
            ITSPHPAst variableId = $varId.start;
            if(variableId.getType()==VariableId){
                variableId.getScope().addToInitialisedSymbols(variableId.getSymbol(), true);
            }
        }*/
    |   ^('?' expression expression expression)
    |   ^(CAST ^(TYPE tMod=. scalarTypes[$tMod]) expression)
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
        // resolving occurs in the type checking phase where overloads are taken into account
    :   ^(FUNCTION_CALL identifier=TYPE_NAME actualParameters)
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

scalarTypesOrUnknown[ITSPHPAst typeModifier] returns [ITypeSymbol type]
    :   scalarTypes[typeModifier] {$type = $scalarTypes.type;}
    |   '?' {$type = null;}
    ;
    
scalarTypes[ITSPHPAst typeModifier] returns [ITypeSymbol type]
//Warning! start duplicated code as in voidType, classInterfaceType and arrayOrResourceOrMixed
@init{
    //TODO rstoll TINS-224 reference phase - resolve types
    /*if(state.backtracking == 1 && $start instanceof ITSPHPErrorAst){
        $type = controller.createErroneousTypeSymbol((ITSPHPErrorAst)$start);
        $start.setSymbol($type);        
        input.consume();
        return retval;
    }*/
}
//Warning! end duplicated code as in voidType, classInterfaceType and arrayOrResourceOrMixed
    :   (   'bool'
        |   'int'
        |   'float'
        |   'string'
        )
        //TODO TINS-224 reference phase - resolve types     
        /*{
            $type = controller.resolveScalarType($start, $typeModifier);
            $start.setSymbol($type);
        }*/
    ;
catch[RecognitionException re]{
    reportError(re);
    recover(input,re);
    //TODO rstoll TINS-224 reference phase - resolve types
    //$type = controller.createErroneousTypeSymbol($start, re);
}
    
classInterfaceType[ITSPHPAst typeModifier] returns [ITypeSymbol type]
//Warning! start duplicated code as in scalarTypes, voidType and arrayOrResourceOrMixed
@init{
    //TODO rstoll TINS-224 reference phase - resolve types
    /*if(state.backtracking == 1 && $start instanceof ITSPHPErrorAst){
        $type = controller.createErroneousTypeSymbol((ITSPHPErrorAst)$start);
        $start.setSymbol($type);        
        input.consume();
        return retval;
    }*/
}
//Warning! end duplicated code as in scalarTypes, voidType and arrayOrResourceOrMixed
    :   TYPE_NAME
        //TODO TINS-224 reference phase - resolve types     
        /*{
            $type = controller.resolveType($start, $typeModifier);
            $start.setSymbol($type);
        }*/
    ;
catch[RecognitionException re]{
    reportError(re);
    recover(input,re);
    //TODO rstoll TINS-224 reference phase - resolve types
    //$type = controller.createErroneousTypeSymbol($start, re);
}
    
arrayType[ITSPHPAst typeModifier] returns [ITypeSymbol type]
//Warning! start duplicated code as in scalarTypes, classInterfaceType and voidType
@init{
    //TODO rstoll TINS-224 reference phase - resolve types
    /*if(state.backtracking == 1 && $start instanceof ITSPHPErrorAst){
        $type = controller.createErroneousTypeSymbol((ITSPHPErrorAst)$start);
        $start.setSymbol($type);
        input.consume();
        return retval;
    }*/
}
//Warning! end duplicated code as in scalarTypes, classInterfaceType and voidType
    :   'array'
        //TODO TINS-224 reference phase - resolve types     
        /*{
            $type = controller.resolvePrimitiveType($start, $typeModifier);
            $start.setSymbol($type);
        }*/
    ;
catch[RecognitionException re]{
    reportError(re);
    recover(input,re);
    //TODO rstoll TINS-224 reference phase - resolve types
    //$type = controller.createErroneousTypeSymbol($start, re);
}