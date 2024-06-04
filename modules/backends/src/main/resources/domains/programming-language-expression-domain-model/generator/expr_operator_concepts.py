#!/usr/bin/env python
# coding: utf-8

# expr_operator_concepts.py

# See also:
# http://localhost:8888/notebooks/Analysis/Expr_operator_concepts.ipynb

# See cpp_concepts_situations(c_code) function below.


from ast import literal_eval
import json
import os.path
import re
import sys

import pycparser   # pip install pycparser
from pycparser import parse_file, c_ast
# from functools import reduce


PRINT_WARN = False


OPERATORS_INFO_JSON = r'expr_operators_info.json'

def load_operators(_dir=''):
    try:
        with open(os.path.join(_dir, OPERATORS_INFO_JSON)) as f:
            operators = json.load(f)
        return operators
    except:
        _dir_path = os.path.dirname(os.path.realpath(__file__)) # dir of current .py file
        return load_operators(_dir=_dir_path)


OPERATORS = load_operators()


# >>> OPERATORS['operators_c++']['operator_?']
    # ({'name': 'operator_?',
    #   'text': '?',
    #   'arity': 'ternary',
    #   'precedence': 16,
    #   'is_operator_with_strict_operands_order': True,
    #   'associativity': 'R'},)
# >>> ...
    # # {'name': 'operator_+=',
    # #  'text': '+=',
    # #  'precedence': 16,
    # #  'arity': 'binary',
    # #  'associativity': 'R'}


def find_concept(op, arity: str, prefix_postfix:str=None, lang='c++'):
    # using `OPERATORS` variable
    concepts = OPERATORS[lang]
    for d in concepts.values():
        if (d['text'] == op and d['arity'] == arity and (
            not prefix_postfix or not (pp := d.get('prefix_postfix')) or pp == prefix_postfix
        )):
            return d  # !! ['name']
    raise ValueError([op, arity, prefix_postfix])




class OpFinder(c_ast.NodeVisitor):
    """ A simple visitor that matches kinds of operations in exprs
    """
    def __init__(self):
        self.op = set()
        self.relations = set()  # previously, "associativity" and "precedence" had been added to self.op

    def handle_assoc_prec_for_binary(self, parent:dict, left:dict, right:dict):
        #       "associativity",
        #       "precedence", ...
        for child in (left, right):
            if child:
                if child['precedence'] == parent['precedence']:
                  if child['associativity'] == parent['associativity']:
                    self.relations.add("associativity")
                    # print(f"associativity:  {parent['name']} ({parent['associativity']}) -- {child['name']} ({child['associativity']})")
                    n_ary = parent['arity']
                    if n_ary in ('unary', 'binary', 'ternary'):  # filter out 'complex' and other (if any)
                        left_right = {'L': "left", 'R': "right"}.get(parent['associativity'])
                        # 'error_base_ternary_having_associativity_right'
                        law_name = f'error_base_{n_ary}_having_associativity_{left_right}'
                        self.relations.add(law_name)
                else:
                    self.relations.add("precedence")

    def visit_ExprList(self, node):  # ,
        # print('%s at %s' % (node.decl.name, node.decl.coord))
        cd = find_concept(',', 'binary')
        if len(node.exprs) > 1:  # function args are ExprList too, so no ',' may actually present
            self.op.add(cd['name'])
        self.visit(node.exprs)
        return cd
    # simple parentheses don't appear in AST

    def visit_FuncCall(self, node):  # ,
        # cd = OPERATORS['operators_c++']['operator_function_call']
        self.op.add('operator_function_call')
        if node.args:
            self.visit(node.args)
        return None
    # simple parentheses don't appear in AST

    def visit_Assignment(self, node):
        # print('%s at %s' % (node.op, node.coord))
        self.op.add((cd := find_concept(node.op, 'binary'))['name'])
        lcd = self.visit(node.lvalue)
        rcd = self.visit(node.rvalue)
        self.handle_assoc_prec_for_binary(cd, lcd, rcd)
        return cd

    def visit_BinaryOp(self, node):
        # print('%s at %s' % (node.decl.name, node.decl.coord))
        self.op.add((cd := find_concept(node.op, 'binary'))['name'])
        lcd = self.visit(node.left)
        rcd = self.visit(node.right)
        self.handle_assoc_prec_for_binary(cd, lcd, rcd)
        # print(node.op, lcd, rcd, sep=' ; ')
        return cd

    def visit_UnaryOp(self, node):
        if node.op in ('p--', 'p++'):
            cd = find_concept(node.op[1:], 'unary', 'postfix')
        elif node.op in ('--', '++'):
            cd = find_concept(node.op, 'unary', 'prefix')
#             print('found prefix-unary:', node.op, cd)
        else:
#             print('found unary:', node.op)
            cd = find_concept(node.op, 'unary')
        self.op.add(cd['name'])
        expr = self.visit(node.expr)
        self.handle_assoc_prec_for_binary(cd, expr, None)
        return cd

    def visit_TernaryOp(self, node):
        self.op.add((cd := find_concept('?', 'ternary'))['name'])
        cond = self.visit(node.cond)
        iftrue = self.visit(node.iftrue)
        iffalse = self.visit(node.iffalse)
        self.handle_assoc_prec_for_binary(cd, cond, iffalse)  # (a ? b : c ? d : e) === (a ? b : c) ? d : e [left assoc.]
        return cd

    def visit_StructRef(self, node):
        self.op.add((cd := find_concept(node.type, 'binary'))['name'])
        name = self.visit(node.name)
        self.handle_assoc_prec_for_binary(cd, name, None)  # (a ? b : c ? d : e) === (a ? b : c) ? d : e [left assoc.]
        return cd

    def visit_ArrayRef(self, node):
        self.op.add((cd := find_concept('[', 'complex'))['name'])
        name = self.visit(node.name)
        subscript = self.visit(node.subscript)
        self.handle_assoc_prec_for_binary(cd, name, None)  # (a ? b : c ? d : e) === (a ? b : c) ? d : e [left assoc.]
        return cd




from pycparser.plyparser import ParseError

c_template = 'void main(){\n%s;\n}'
GROUPING_PARENS_RE = re.compile(r'\W\s\(')  # open parenthese that are not func call (exactly 1 space is required)

def cpp_concepts_situations(c_code):
    # dump to file as parser supports files only...
    with open('tmp.c', 'w') as f: f.write(c_template % c_code)
    try:
        myast = pycparser.parse_file('tmp.c')
    except ParseError as e:
        if PRINT_WARN:
            print('WARN: cpp_concepts_situations( "%s" ) -->' % c_code, e.__class__.__name__, ':')
            print(e)
        return ((), ())
    (of := OpFinder()).visit(myast)
    if GROUPING_PARENS_RE.search(c_code):
        of.op.add('operator_(')  # parens that are not func call
    return sorted(of.op, key=lambda s: (s.count('_'), s)), sorted(of.relations)




def _test_it():
    # new_concepts, new_situations = cpp_concepts_situations(expr_string)
    print(cpp_concepts_situations('b += b -= a - 8 * 7'))
    # bad input
    print(cpp_concepts_situations(' += -= '))


def interactive_main():
    """ For each line of input, print on line of situations (law/violation names)"""
    while (expr_string := sys.stdin.readline().strip()):

        new_concepts, new_situations = cpp_concepts_situations(expr_string)

        output = " ".join(new_concepts)
        sys.stdout.write(output + '\n')
        sys.stdout.flush()

        output = " ".join(new_situations)
        sys.stdout.write(output + '\n')
        sys.stdout.flush()
    # done: got empty input


if __name__ == "__main__":
    # _test_it()
    if '--interactive' in sys.argv:
        interactive_main()
    else:
        print("For each line of С expression on input, prints one line of concept names and one more line of situations (law/violation names).")
        print("Usage:")
        print("python", sys.argv[0], '--interactive')
        print()


