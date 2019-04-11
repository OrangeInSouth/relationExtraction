from django.shortcuts import render

# Create your views here.
from django.shortcuts import HttpResponse  # 导入HttpResponse模块
from django.shortcuts import render#导入render模块
'''
def index(request):  # request是必须带的实例。类似class下方法必须带self一样
    return HttpResponse("Hello World!!")  # 通过HttpResponse模块直接返回字符串到前端页面
'''
from django.shortcuts import render  # 导入render模块
from REweb import relation_triple_extraction_RULE
import traceback

def index(request):
    sentence = request.POST.get('sentence', None)
    REresult = []
    try:    # 异常情况是第一次打开网页时，页面显示空的结果（REresult为空）
        with open('./REweb/input.txt','w') as f:    # 读取用户输入，记录在input.txt中
            f.write(sentence)
        result=relation_triple_extraction_RULE.main()      # 三元组抽取模型读input.txt文件，把抽取结果写在output_jiang.txt中
        '''
        with open('./REweb/output_jiang.txt', 'r',encoding='utf-8') as fr:   # 输出文件中每行是一个抽取到的三元组结果，添加到REresult中
            for line in fr.readlines():
                REresult.append(line)
        '''
        REresult=result
        # return HttpResponse(str(output_S))
        if not REresult:    # 没抽到时
            REresult.append('抱歉，没有抽取到三元组！')
    except:
        traceback.print_exc()

    return render(request,'index.html',{'sen':REresult})


if __name__ == '__main__':
    index()