/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */
 
 /* This file was created based on TinsPHPDefinitionWalker.g - the tree grammar file for the definition phase of
  * TSPHP's type checker - and reuses TSPHP's AST class as well as other classes/files related to the AST generation.
  * TSPHP is also licenced under the Apache License 2.0
  * For more information see http://tsphp.ch/wiki/display/TSPHP/License
  */

tree grammar TinsPHPDefinitionWalker;
options {
    tokenVocab = TinsPHP;
    ASTLabelType = ITSPHPAst;
    filter = true;        
}

@header{
/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.antlr;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.inference_engine.IDefinitionPhaseController;
import ch.tsphp.tinsphp.inference_engine.scopes.INamespaceScope;

}

@members {

private IDefinitionPhaseController definer;
private IScope currentScope;


public TinsPHPDefinitionWalker(TreeNodeStream input, IDefinitionPhaseController theDefiner) {
    this(input);
    definer = theDefiner;    
}

}

topdown
        //scoped symbols
    :   namespaceDefinition   
    //TODO rstoll TINS-163 definition phase - use
    //|   useDefinitionList
    //TODO rstoll TINS-161 inference OOP
    //|   interfaceDefinition
    //|   classDefinition
    //|   constructDefinition
    
    //TODO rstoll TINS-155 definition phase - functions
    //|   methodFunctionDefinition
    //TODO rstoll TINS-162 definition phase - scopes
    //|   blockConditional
    //|   foreachLoop
    
        //symbols
    //TODO rstoll TINS-156 definition phase - constants
    //|   constantDefinitionList
    //TODO rstoll TINS-154 definition phase - variables
    //|   parameterDeclarationList
    //variables are implicitly defined in PHP
    //|   variableDeclarationList
    //TODO rstoll TINS-162 definition phase - scopes
    /*|   methodFunctionCall
    |   atom
    |   constant
    |   returnBreakContinue
    */
    ;

bottomup
    :   exitNamespace
    //TODO rstoll TINS-162 definition phase - scopes
    //|   exitScope
    ;

exitNamespace
    :   Namespace
        {currentScope = currentScope.getEnclosingScope().getEnclosingScope();}
    ;
    
//TODO rstoll TINS-162 definition phase - scopes
/*
exitScope
    :   (   //TODO rstoll TINS-161 inference OOP
            // 'interface'
        //|   'class'
        //|   '__construct'
        //|   METHOD_DECLARATION
            Function
        |   BLOCK_CONDITIONAL
        |   Foreach
        ) 
        {
            //only get enclosing scope if a scope was defined - might not be the case due to syntax errors
            if(!(currentScope instanceof INamespaceScope)){
                currentScope = currentScope.getEnclosingScope();
            }
        }
    ;   
    */
    
namespaceDefinition
    :   ^(Namespace t=(TYPE_NAME|DEFAULT_NAMESPACE) .)
        {currentScope = definer.defineNamespace($t.text); }
    ;

//TODO rstoll TINS-163 definition phase - use
/*useDefinitionList
    :   ^('use'    useDeclaration+)
    ;
    
useDeclaration
    :   ^(USE_DECLARATION type=TYPE_NAME alias=Identifier)
        {definer.defineUse((INamespaceScope) currentScope, $type, $alias);}
    ;
 */

//TODO rstoll TINS-161 inference OOP    
/*interfaceDefinition
    :   ^('interface' iMod=. identifier=Identifier extIds=. .)
        {currentScope = definer.defineInterface(currentScope, $iMod, $identifier, $extIds); }
    ;
    
classDefinition
    :   ^('class' cMod=. identifier=Identifier extId=. implIds=. .)
        {currentScope = definer.defineClass(currentScope, $cMod, $identifier, $extId, $implIds); }    
    ;
    
constructDefinition
    :   ^(identifier='__construct' mMod=.  ^(TYPE rtMod=. returnType=.) . .)
        {currentScope = definer.defineConstruct(currentScope, $mMod, $rtMod, $returnType, $identifier);}
    ;
*/

//TODO rstoll TINS-162 definition phase - scopes
/*
methodFunctionDefinition
    :   ^(  //TODO rstoll TINS-161 inference OOP  
            //(   METHOD_DECLARATION
               Function
            //)
            mMod=. ^(TYPE rtMod=. returnType=.) Identifier . .
        )
        {currentScope = definer.defineMethod(currentScope,$mMod, $rtMod, $returnType, $Identifier); }
    ;
 
blockConditional
    :   ^(block=BLOCK_CONDITIONAL .*)
        {
            currentScope = definer.defineConditionalScope(currentScope);
            $block.setScope(currentScope);
        }    
    ;
    
foreachLoop
    :   ^(Foreach .*)
        {
            currentScope = definer.defineConditionalScope(currentScope);
            $Foreach.setScope(currentScope);
        }    
    ;
*/

//TODO rstoll TINS-156 definition phase - constants    
/*constantDefinitionList
    :   ^(CONSTANT_DECLARATION_LIST ^(TYPE tMod=. type=.) constantDeclaration[$tMod, $type]+)
    ;

constantDeclaration[ITSPHPAst tMod, ITSPHPAst type]
    :   ^(identifier=Identifier .)
        { definer.defineConstant(currentScope,$tMod, $type,$identifier); }
    ;
*/

//TODO rstoll TINS-154 definition phase - variables
/*parameterDeclarationList
    :   ^(PARAMETER_LIST parameterDeclaration+)
    ;

parameterDeclaration
    :   ^(PARAMETER_DECLARATION
            ^(TYPE tMod=. type=.) variableDeclaration[$tMod,$type]
        )
    ;
    
variableDeclaration[ITSPHPAst tMod, ITSPHPAst type]
    :
        (   ^(variableId=VariableId .)
        |   variableId=VariableId
        )
        {definer.defineVariable(currentScope, $tMod, $type, $variableId);}
    ;
*/
  
//TODO rstoll TINS-162 definition phase - scopes 
/*    
methodFunctionCall
    :   //TODO rstoll TINS-161 inference OOP    
    //    ^(METHOD_CALL callee=. identifier=Identifier .)
    //    {$callee.setScope(currentScope);}
    //|   ^(METHOD_CALL_STATIC callee=. identifier=Identifier .)
    //    {$callee.setScope(currentScope);}
    //|   ^(METHOD_CALL_POSTFIX identifier=Identifier .)
    
        ^(FUNCTION_CALL identifier=TYPE_NAME .)
        {$identifier.setScope(currentScope);}
    ;

atom    
    :   (   identifier='$this'
        |   identifier=VariableId
        //TODO rstoll TINS-161 inference OOP  
        //|    identifier='parent'
        //|    identifier='self'
            //self and parent are already covered above
        //|    ^(CLASS_STATIC_ACCESS identifier=(TYPE_NAME|'self'|'parent') .)
        
        |    ^(CAST ^(TYPE . type=primitiveTypesWithoutResource) .) {$identifier=$type.start;}
        |    ^('instanceof' . (identifier=VariableId | identifier=TYPE_NAME))
        |    ^('new' identifier=TYPE_NAME .)
        )
        {$identifier.setScope(currentScope);}
    ;

primitiveTypesWithoutResource
    :   'bool'
    |   'int'
    |   'float'
    |   'string'
    |   'array'
    ;


constant
    :   cst=CONSTANT
        {$cst.setScope(currentScope);}
    ;

returnBreakContinue
    :   (   Return
        |   Break
        |   Continue
        )
        {$start.setScope(currentScope);}
    ;
*/