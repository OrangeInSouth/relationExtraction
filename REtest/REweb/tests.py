def include(a, b):
    if a in b or b in a:
        return True
    else:
        return False


print(include('你好', '你好吗'))
print(include('你好吗', '你好'))
