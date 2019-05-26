from django.shortcuts import render  # 导入render模块
from REweb import relation_triple_extraction_RULE
import traceback

def index(request):
    sentence = request.POST.get('sentence', None)
    REresult = []
    # if sentence ==None:
    #     return HttpResponseRedirect("/")
    if sentence==None:    # 第一次打开网页时，页面显示空的结果
        return render(request,'index.html',{'sen':None})
    try:
        result=relation_triple_extraction_RULE.main(sentence)      # 三元组抽取模型读input.txt文件，把抽取结果写在output_jiang.txt中
        REresult=result
        if not REresult:    # 没抽到时
            REresult.append('抱歉，没有抽取到三元组！')
    except:
        traceback.print_exc()

    return render(request,'index.html',{'sen':REresult})


if __name__ == '__main__':
    index()
