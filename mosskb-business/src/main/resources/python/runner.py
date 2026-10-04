"""One JSON request per disposable container. No host credentials or mounts."""
import ast
import contextlib
import io
import json
import sys


class LimitedOutput(io.StringIO):
    def write(self, value):
        room = max(0, 8192 - self.tell())
        super().write(value[:room])
        return len(value)


def run(request):
    code = request.get("code", "")
    tree = ast.parse(code, filename="function.py")
    if request.get("mode") == "lint":
        return []
    functions = [node.name for node in tree.body if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))]
    if not functions:
        raise ValueError("代码需要至少一个顶层函数")
    entry = request.get("entrypoint") or functions[-1]
    if entry not in functions:
        raise ValueError("入口函数不存在")
    namespace = {"__name__": "mosskb_function"}
    output = LimitedOutput()
    with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
        exec(compile(tree, "function.py", "exec"), namespace, namespace)
        value = namespace[entry](**request.get("params", {}))
        import inspect
        if inspect.isawaitable(value):
            import asyncio
            value = asyncio.run(value)
    return value


def main():
    try:
        request = json.loads(sys.stdin.buffer.read(131073))
        try:
            data = run(request)
        except SyntaxError as error:
            if request.get("mode") != "lint":
                raise
            data = [{"line": error.lineno, "column": max(0, (error.offset or 1) - 1),
                     "endLine": error.end_lineno, "endColumn": error.end_offset,
                     "message": error.msg, "type": "error"}]
        response = json.dumps({"ok": True, "data": data}, ensure_ascii=False, allow_nan=False)
        if len(response.encode("utf-8")) > 262144:
            raise ValueError("函数返回结果超过 256 KiB")
    except BaseException as error:
        response = json.dumps({"ok": False, "error": str(error)[:1000]}, ensure_ascii=False)
    sys.stdout.write(response)


if __name__ == "__main__":
    main()
