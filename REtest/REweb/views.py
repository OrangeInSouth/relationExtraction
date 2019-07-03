import os
from django.shortcuts import render  # 导入render模块
from pyltp import Segmentor, Postagger, Parser, NamedEntityRecognizer
from django.template import RequestContext
from django.shortcuts import render_to_response
from REweb import relation_triple_extraction_RULE
import traceback

MODELDIR = "/Users/yongjieliu/pycharm/graduate/data/ltp_data_v3.4.0/"

print("正在加载LTP模型... ...")

segmentor = Segmentor()

p = os.path.join(MODELDIR, "cws.model")
segmentor.load(p)
print(p)

postagger = Postagger()
postagger.load(os.path.join(MODELDIR, "pos.model"))

parser = Parser()
parser.load(os.path.join(MODELDIR, "parser.model"))

recognizer = NamedEntityRecognizer()
recognizer.load(os.path.join(MODELDIR, "ner.model"))

# labeller = SementicRoleLabeller()
# labeller.load(os.path.join(MODELDIR, "srl/"))

print("加载模型完毕。")


def index(request):
    return render(request, "index.html", {'origin_sentence': "", 'results': []})


def getTriple(request):
    results = []
    if request.POST:
        sentences = request.POST['sentence']
        results = relation_triple_extraction_RULE.main(sentences, segmentor, postagger, recognizer, parser)
        # results = [
        #
        #     ['0', '德国人再次展现了他们在世界杯上打好开局的能力，4-0战胜葡萄牙后，德国球迷也是一片欢腾。作为德国国家队最大牌的球迷之一——'], ['1', '德国'], ['2', '女总理'],
        #     ['3', '默克尔'],
        #     ['0', '自然不会错过这样的盛宴。“默大妈”不仅现场督战，更在赛后造访了更衣室。']

        # ]
        return render(request, "index.html", {'origin_sentence': sentences, 'results': results})
    else:
        return render(request, "index.html", {'origin_sentence': "", 'results': []})

# if __name__ == '__main__':
#     index()
